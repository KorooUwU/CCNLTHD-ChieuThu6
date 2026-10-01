package com.notification.dispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class NotificationDispatcherApplication {

	public static void main(String[] args) {
		// PostgreSQL may reject the legacy timezone name supplied by Windows.
		if ("Asia/Saigon".equals(TimeZone.getDefault().getID())) {
			TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		}
		SpringApplication.run(NotificationDispatcherApplication.class, args);
	}

}
