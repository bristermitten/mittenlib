// @ts-check

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'MittenLib',
  tagline: 'Modern Java Configurations',
  url: 'https://bristermitten.github.io',
  baseUrl: '/mittenlib/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',

  organizationName: 'bristermitten',
  projectName: 'mittenlib',

  markdown: {
    mermaid: true,
  },
  themes: ['@docusaurus/theme-mermaid'],

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/bristermitten/mittenlib/tree/master/website/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'MittenLib',
        items: [
          {
            type: 'doc',
            docId: 'intro',
            position: 'left',
            label: 'Guides',
          },
          {
            href: 'pathname:///javadoc/index.html',
            label: 'Javadoc',
            position: 'right',
          },
          {
            href: 'https://github.com/bristermitten/mittenlib',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      prism: {
        additionalLanguages: ['java', 'groovy', 'kotlin'],
      },
    }),
};

module.exports = config;
