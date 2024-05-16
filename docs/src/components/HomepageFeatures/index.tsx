import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
    title: string;
    imageSrc: string;
    description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
    {
        title: 'Decentralization',
        imageSrc: 'img/many-shields.png',
        description: (
                <>
                    Empowers users with complete control over their digital identity by eliminating the need for
                    centralized authorities.
                </>
        ),
    },
    {
        title: 'Privacy Preservation',
        imageSrc: 'img/lock.png',
        description: (
                <>
                    Safeguard privacy by selectively sharing identity information. Enable
                    individuals to disclose only necessary data for each interaction, preventing unnecessary exposure
                    and minimizing the risk of identity theft or surveillance.
                </>
        ),
    },
    {
        title: 'Interoperability',
        imageSrc: 'img/interoperability.png',
        description: (
                <>
                    Seamlessly integrate your digital identity across diverse platforms and services. Effortless
                    exchange of identity credentials, smooth interactions across different ecosystems and
                    enhancing user experience.
                </>
        ),
    },
];

function Feature({title, imageSrc, description}: FeatureItem) {
    return (
            <div className={clsx('col col--4')}>
                <div className="text--center">
                    <img src={imageSrc} alt="Image"/>
                </div>
                <div className="text--center padding-horiz--md">
                    <Heading className={styles.featureHeading} as="h3">{title}</Heading>
                    <p>{description}</p>
                </div>
            </div>
    );
}

export default function HomepageFeatures(): JSX.Element {
    return (
            <section className={styles.features}>
                <div className="container">
                    <div className="row">
                        {FeatureList.map((props, idx) => (
                                <Feature key={idx} {...props} />
                        ))}
                    </div>
                </div>
            </section>
    );
}
