import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
  tutorialSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Tutorials',
      items: [
        'tutorial/getting-started',
        {
          type: 'category',
          label: 'Config System',
          items: [
            'tutorial/config-system/first-config',
            'tutorial/config-system/adding-features',
            'tutorial/config-system/loading-and-usage',
          ],
        }
      ],
    },
    {
      type: 'category',
      label: 'Config Reference',
      link: {
        type: 'doc',
        id: 'config/index',
      },
      items: [
        'config/getting-started',
        'config/data-types',
        'config/naming-keys',
        'config/validation',
        'config/guice-integration',
        'config/persistence',

      ],
    },
    {
      type: 'category',
      label: 'Other Modules',
      items: [
        'modules/commands',
        'modules/gui',
        'modules/minimessage',
        'modules/papi',
        'modules/record-generator',
      ],
    },
    {
      type: 'category',
      label: 'Advanced',
      items: [
        'config/core-system',
      ],
    },
  ],
};

export default sidebars;
