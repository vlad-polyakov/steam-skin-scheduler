package com.steam.skin.scheduler;

import com.steam.skin.scheduler.connector.SteamClientConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SteamSkinSchedulerApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SteamSkinSchedulerApplication.class, args);
	}

}
