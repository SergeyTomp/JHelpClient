package jhelp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:app.properties")
public class JavaConfig {

    @Value("${server.socket.port}")
    private int port;

    @Bean
    public JHelpClient jHelpClient(){
        JHelpClient jHelpClient = new JHelpClient();
        jHelpClient.setSERVER_PORT(port);
        return new JHelpClient();
    }
}
