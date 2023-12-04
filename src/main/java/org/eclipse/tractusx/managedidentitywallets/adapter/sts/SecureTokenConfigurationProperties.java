package org.eclipse.tractusx.managedidentitywallets.adapter.sts;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sts")
public record SecureTokenConfigurationProperties(Duration tokenDuration) {
}
