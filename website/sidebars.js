// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Configuration',
      link: {
        type: 'generated-index',
      },
      collapsed: false,
      items: [
        'config/index',
        'config/validation',
        'config/pipeline',
      ],
    },
  ],
};

module.exports = sidebars;
