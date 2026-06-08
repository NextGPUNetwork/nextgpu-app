package ai.nextgpu.common.model;

import org.springframework.data.annotation.Id;

public class Manufacturer extends BaseObject {
    @Id
    private String name;
}
