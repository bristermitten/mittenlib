import React, {useState, useEffect} from 'react';
import Link from '@docusaurus/Link';
import {Tooltip} from 'react-tooltip';
import useBaseUrl from '@docusaurus/useBaseUrl';
import type {JavadocType} from '@site/src/data/javadoc-types';

// Dynamically import the JSON so it doesn't block initial page load
const fetchMeta = () => import('@site/src/data/javadoc-meta.json').catch(() => ({default: {}}));

interface BaseJavadocProps<Self extends { type: unknown }> {
    member?: string;
    generics?: (Self["type"])[];
    children?: React.ReactNode;
}

interface JavadocProps extends BaseJavadocProps<JavadocProps> {
    type: JavadocType;
}

interface ExternalJavadocProps extends BaseJavadocProps<ExternalJavadocProps> {
    type: JavadocType | (string & {});
}

const ExternalLinkIcon = () => (
    <svg width="12" height="12" aria-hidden="true" viewBox="0 0 24 24"
         style={{marginLeft: '0.3em', verticalAlign: 'middle', opacity: 0.6}}>
        <path fill="currentColor"
              d="M21 13v10h-21v-19h12v2h-10v15h17v-8h2zm3-12h-10.988l4.035 4-6.977 7.07 2.828 2.828 6.977-7.07 4.125 4.172v-11z"></path>
    </svg>
);

function getJavadocUrl(type: string, member?: string): {
    url: string;
    isExternal: boolean;
    path: string;
    hash: string
} {
    const path = type.replace(/\./g, '/');
    const hash = member ? `#${member.replace(/\(/g, '-').replace(/\)/g, '-')}` : '';

    if (type.startsWith('java.') || type.startsWith('javax.')) {
        return {
            url: `https://docs.oracle.com/en/java/javase/21/docs/api/java.base/${path}.html${hash}`,
            isExternal: true,
            path,
            hash
        };
    }

    if (type.startsWith('org.bukkit.') || type.startsWith('org.spigotmc.')) {
        return {url: `https://helpch.at/docs/1.8.8/${path}.html${hash}`, isExternal: true, path, hash};
    }

    if (type.startsWith('net.kyori.adventure.')) {
        return {url: `https://jd.advntr.dev/api/latest/${path}.html${hash}`, isExternal: true, path, hash};
    }

    return {url: '', isExternal: false, path, hash};
}

function JavadocInternal({type, member, generics, children}: JavadocProps | ExternalJavadocProps) {
    const [meta, setMeta] = useState<Record<string, { description: string }> | null>(null);

    useEffect(() => {
        fetchMeta().then((module) => setMeta(module.default));
    }, []);

    // Extract FQN and Display ID from "DisplayID [FQN]" format if present
    let fqn = type;
    let displayId = type;

    if (type.includes(' [')) {
        const parts = type.split(' [');
        displayId = parts[0];
        fqn = parts[1].split(']')[0];
    }

    const {url, isExternal, path, hash} = getJavadocUrl(fqn, member);
    const localUrl = useBaseUrl(`/javadoc/${path}.html${hash}`);
    const href = isExternal ? url : `pathname://${localUrl}`;

    // Extract a cleaner simple name from the FQN if it's just a class
    // For members, displayId already has a nice format (e.g., Class#member)
    const simpleName = displayId.includes('#') ? displayId.split('#')[0] : displayId;

    const renderGenerics = () => {
        if (!generics || generics.length === 0) return null;
        return (
            <>
                &lt;
                {generics.map((gen, idx) => (
                    <React.Fragment key={idx}>
                        {idx > 0 && ', '}
                        <ExternalJavadoc type={gen}/>
                    </React.Fragment>
                ))}
                &gt;
            </>
        );
    }

    const displayText = children || (
        <>
            {displayId}
            {renderGenerics()}
        </>
    );

    const tooltipId = `javadoc-tooltip-${fqn}-${member || 'class'}`.replace(/[^a-zA-Z0-9-]/g, '-');

    const metaKey = member ? `${fqn}#${member}` : fqn;
    const typeData = meta?.[metaKey] || meta?.[fqn];
    const description = typeData?.description;

    return (
        <>
            <Link
                href={href}
                title={`View Javadoc for ${fqn}`}
                data-tooltip-id={tooltipId}
                style={{textDecoration: 'underline', textDecorationStyle: 'dotted', fontWeight: 500}}
                target={isExternal ? '_blank' : undefined}
                rel={isExternal ? 'noopener noreferrer' : undefined}
            >
                <code>{displayText}</code>
                {isExternal && <ExternalLinkIcon/>}
            </Link>

            {description && (
                <Tooltip id={tooltipId} place="top" style={{maxWidth: '400px', zIndex: 1000}}>
                    <div style={{fontSize: '0.85em', lineHeight: 1.4}}>
                        <div style={{
                            fontWeight: 'bold',
                            borderBottom: '1px solid rgba(255,255,255,0.2)',
                            marginBottom: '4px',
                            paddingBottom: '2px'
                        }}>
                            {displayId}
                        </div>
                        <div style={{whiteSpace: 'pre-wrap'}}>{description}</div>
                    </div>
                </Tooltip>
            )}
        </>
    );
}

/**
 * A Javadoc link for types known to the MittenLib project (extracted from the local MittenLib docs or JDK/Spigot/Adventure).
 * Provides full autocompletion for all known classes and members.
 */
export function Javadoc(props: JavadocProps) {
    return <JavadocInternal {...props} />;
}

/**
 * A Javadoc link for any arbitrary type.
 * Does not provide autocompletion for the type name, but still attempts to route correctly if it matches known external patterns.
 */
export function ExternalJavadoc(props: ExternalJavadocProps) {
    return <JavadocInternal {...props} />;
}

export default Javadoc;
