package create.volume.in.instance;

import io.micronaut.function.client.FunctionClient;
import io.micronaut.http.annotation.Body;
import io.reactivex.Single;
import javax.inject.Named;

@FunctionClient
public interface CreateVolumeInInstanceClient {

    @Named("create-volume-in-instance")
    Single<CreateVolumeInInstance> apply(@Body CreateVolumeInInstance body);

}
