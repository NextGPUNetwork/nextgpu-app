package ai.nextgpu.common.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest(classes = {GpuTest.class})
class GpuTest {

    @Test
    void shouldNotSetNonGpuType() {
        Gpu gpu = new Gpu();
        gpu.setManufacturer("nvidia");
        gpu.setType(MemoryType.DDR2);
        assertSame(MemoryType.UNKNOWN, gpu.getType());
    }
}
