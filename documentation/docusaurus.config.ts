import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
    title: 'Managed Identity Wallets',
    tagline: 'Managed Self-Sovereign Identity Wallets',
    favicon: 'img/favicon.ico',

    // Set the production url of your site here
    url: 'https://github.com',
    // Set the /<baseUrl>/ pathname under which your site is served
    // For GitHub pages deployment, it is often '/<projectName>/'
    baseUrl: '/eclipse-tractusx/managed-identity-wallet/',

    // GitHub pages deployment config.
    // If you aren't using GitHub pages, you don't need these.
    organizationName: 'eclipse-tractusx', // Usually your GitHub org/user name.
    projectName: 'managed-identity-wallet', // Usually your repo name.

    onBrokenLinks: 'throw',
    onBrokenMarkdownLinks: 'warn',

    // Even if you don't use internationalization, you can use this field to set
    // useful metadata like html lang. For example, if your site is Chinese, you
    // may want to replace "en" with "zh-Hans".
    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    presets: [
        [
            'classic',
            {
                docs: {
                    sidebarPath: './sidebars.ts'
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
            title: 'Managed Identity Wallets',
            logo: {
                alt: 'Tractus-X Logo',
                src: 'img/logo.png',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'ssiSidebar',
                    position: 'left',
                    label: 'Self-Sovereign-Identity',
                },
                {
                    type: 'docSidebar',
                    sidebarId: 'developmentSidebar',
                    position: 'left',
                    label: 'Development',
                },
                {
                    type: 'docSidebar',
                    sidebarId: 'operationsSidebar',
                    position: 'left',
                    label: 'Operations',
                },
                {
                    href: 'https://github.com/eclipse-tractusx/managed-identity-wallet',
                    label: 'GitHub',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: 'dark',
            links: [
                {
                    title: 'Docs',
                    items: [
                        {
                            label: 'Self-Sovereign-Identity',
                            to: '/docs/ssi/Introduction',
                        },
                        {
                            label: 'Development',
                            to: '/docs/development/Introduction',
                        },
                        {
                            label: 'Operations',
                            to: '/docs/operations/Introduction',
                        }
                    ],
                },
                {
                    title: 'Community',
                    items: [
                        {
                            label: 'Eclipse Tractus-X',
                            href: 'https://eclipse-tractusx.github.io/',
                        },
                    ],
                },
                {
                    title: 'More',
                    items: [
                        {
                            label: 'GitHub',
                            href: 'https://github.com/facebook/docusaurus',
                        },
                        {
                            label: 'Catena-X Automotive Network',
                            href: 'https://catena-x.net',
                        }
                    ],
                },
            ],
            copyright: `Copyright © ${new Date().getFullYear()} Tractus-X`,
        },
        prism: {
            theme: prismThemes.github,
            darkTheme: prismThemes.dracula,
        },
    } satisfies Preset.ThemeConfig,
};

export default config;
