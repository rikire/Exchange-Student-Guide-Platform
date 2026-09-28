package in.ac.iitm.guide.media.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MediaSettings.class)
class MediaConfiguration {}
