package create.volume.in.instance;

import io.micronaut.core.annotation.*;

@Introspected
public class LeastUsedInstance {
	private String ip;
	private String user;
	private String token;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}

