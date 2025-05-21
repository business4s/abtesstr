import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
    title: 'Abtesstr',
    tagline: 'Minimalistic A/B testing engine',
    favicon: 'img/favicon.ico',

    // GitHub pages deployment config.
    url: 'https://business4s.github.io/',
    baseUrl: '/abtesstr/',
    organizationName: 'business4s',
    projectName: 'abtesstr',
    trailingSlash: true,

    onBrokenLinks: 'throw',
    onBrokenMarkdownLinks: 'warn',

    // Even if you don't use internationalization, you can use this field to set
    // useful metadata like html lang. For example, if your site is Chinese, you
    // may want to replace "en" with "zh-Hans".
    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    markdown: {
        mermaid: true,
    },
    themes: ['@docusaurus/theme-mermaid'],

    presets: [
        [
            'classic',
            {
                docs: {
                    sidebarPath: './sidebars.ts',
                    editUrl: 'https://github.com/business4s/abtesstr/tree/main/website',
                    beforeDefaultRemarkPlugins: [
                        [
                            require('remark-code-snippets'),
                            {baseDir: "../abtesstr-examples/src/"}
                        ]
                    ],
                },
                theme: {
                    customCss: './src/css/custom.css',
                },
            } satisfies Preset.Options,
        ],
    ],

    themeConfig: {
        // Replace with your project's social card
        image: 'img/docusaurus-social-card.jpg',
        navbar: {
            title: 'Abtesstr',
            logo: {
                alt: 'Abtesstr Logo',
                src: 'img/abtesstr-logo.drawio.svg',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'tutorialSidebar',
                    position: 'left',
                    label: 'Docs',
                },
                {
                    href: 'https://business4s.org',
                    label: 'Business4s',
                    position: 'right',
                },
                {
                    href: 'https://github.com/business4s/abtesstr',
                    label: 'GitHub',
                    position: 'right',
                    // html: `<i class="fab fa-github" aria-hidden="true"></i>`,
                },
            ],
        },
        footer: {
            style: 'dark',
            links: [],
            // copyright: `Copyright © ${new Date().getFullYear()} My Project, Inc. Built with Docusaurus.`,
        },
        prism: {
            theme: prismThemes.github,
            darkTheme: prismThemes.dracula,
            additionalLanguages: ['java', 'scala', "json"]
        },
        customFields: {
            abtesstrVersion: process.env.ABTESSTR_VERSION,
        },
        // algolia: {
        //     appId: 'IMCN9UXKWU',
        //     apiKey: '6abd8b572e53e72a85a9283c552438b7',
        //     indexName: 'business4s',
        //     searchPagePath: 'search',
        // },
    } satisfies Preset.ThemeConfig,
};

export default config;
