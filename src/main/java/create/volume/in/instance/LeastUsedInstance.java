package create.volume.in.instance;

import io.micronaut.core.annotation.*;

@Introspected
public class LeastUsedInstance {
	private String ip;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
}

