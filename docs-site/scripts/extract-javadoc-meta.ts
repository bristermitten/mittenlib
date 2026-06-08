import fs from 'fs';
import path from 'path';
import * as cheerio from 'cheerio';

const javadocDir = path.resolve('./static/javadoc');
const outputFile = path.resolve('./src/data/javadoc-meta.json');
const meta: Record<string, { description: string }> = {};

function walkDir(dir: string) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      walkDir(fullPath);
    } else if (fullPath.endsWith('.html') && !fullPath.includes('class-use') && !fullPath.includes('index-all') && !fullPath.includes('package-summary')) {
      extractMeta(fullPath);
    }
  }
}

function extractMeta(filePath: string) {
  const html = fs.readFileSync(filePath, 'utf-8');
  const $ = cheerio.load(html);

  const titleText = $('title').text(); 
  if (!titleText) return;
  
  // Package name and Class hierarchy
  const breadcrumbs = $('ol.sub-nav-list > li').map((_, el) => $(el).text().trim()).get();
  if (breadcrumbs.length < 2) return;

  const packageName = breadcrumbs[0];
  const classHierarchy = breadcrumbs.slice(1);
  const className = classHierarchy.join('.');
  
  let fqn = className;
  if (packageName && packageName !== 'Unnamed Package') {
      fqn = `${packageName}.${className}`;
  }

  // Description
  const description = $('section.class-description div.block').first().text().trim();

  // ALWAYS add the class to metadata, even if it has no description
  meta[fqn] = { description: description || '' };

  // Extract Methods, Fields, Constructors, Elements
  $('section.detail').each((_, el) => {
    const $el = $(el);
    const memberName = $el.attr('id'); // e.g. "requireDynamicInitialization()"
    if (!memberName) return;

    const memberDescription = $el.find('div.block').first().text().trim();
    // For members, we only add if there's a description to keep the list sane?
    // Actually, let's add them all for autocompletion completeness.
    meta[`${fqn}#${memberName}`] = { description: memberDescription || '' };
  });
}

walkDir(javadocDir);

// ensure directory exists
fs.mkdirSync(path.dirname(outputFile), { recursive: true });

fs.writeFileSync(outputFile, JSON.stringify(meta, null, 2));

// Generate TypeScript definitions for autocompletion
const typesFile = path.resolve('./src/data/javadoc-types.ts');
const fqns = Object.keys(meta).sort();

const types = fqns.map(fqn => {
    const parts = fqn.split('#');
    const classFqn = parts[0];
    let memberPart = parts[1];
    
    // For inner classes, the FQN might be a.b.C.D. 
    // We want the "Simple" name to be "C.D"
    const classParts = classFqn.split('.');
    let simpleClassName = classParts[classParts.length - 1];
    
    // Check if it's likely an inner class (last parts start with uppercase)
    for (let i = classParts.length - 2; i >= 0; i--) {
        if (classParts[i][0] === classParts[i][0].toUpperCase()) {
            simpleClassName = classParts[i] + '.' + simpleClassName;
        } else {
            break;
        }
    }

    if (memberPart && memberPart.includes('(')) {
        const name = memberPart.split('(')[0];
        const params = memberPart.split('(')[1].split(')')[0];
        if (params) {
            const simplified = params.split(',').map(p => {
                const isVarargs = p.endsWith('...');
                const type = isVarargs ? p.slice(0, -3) : p;
                return (type.split('.').pop() || type) + (isVarargs ? '...' : '');
            }).join(', ');
            memberPart = name + '(' + simplified + ')';
        }
    }
    
    const displayId = memberPart ? simpleClassName + '#' + memberPart : simpleClassName;
    
    const escapedFqn = fqn.replace(/'/g, "\\'");
    return "  | '" + displayId + " [" + escapedFqn + "]'";
}).join('\n');

const typeDefinition = "// AUTO-GENERATED - DO NOT EDIT\n\nexport type JavadocType =\n" +
  (fqns.length > 0 ? types : '  string') + ";\n";
fs.writeFileSync(typesFile, typeDefinition);

console.log(`Extracted metadata for ${fqns.length} elements.`);
