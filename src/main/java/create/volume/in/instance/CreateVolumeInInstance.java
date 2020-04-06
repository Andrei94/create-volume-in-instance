package create.volume.in.instance;

import io.micronaut.core.annotation.*;

@Introspected
public class CreateVolumeInInstance {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

