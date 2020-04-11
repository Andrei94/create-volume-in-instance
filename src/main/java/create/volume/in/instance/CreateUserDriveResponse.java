package create.volume.in.instance;

import io.micronaut.core.annotation.*;

@Introspected
public class CreateUserDriveResponse {
	private String ip;
	private String volumeId;
	private String token;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getVolumeId() {
		return volumeId;
	}

	public void setVolumeId(String volumeId) {
		this.volumeId = volumeId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}

