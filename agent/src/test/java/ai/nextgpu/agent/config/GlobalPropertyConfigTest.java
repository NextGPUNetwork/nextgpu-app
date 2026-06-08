package ai.nextgpu.agent.config;

import ai.nextgpu.agent.service.NextGpuWebService;
import ai.nextgpu.common.model.GlobalProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ai.nextgpu.agent.service.BaseTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = BaseTest.TestConfig.class)
class GlobalPropertyConfigTest extends BaseTest {

    @Autowired
    private GlobalPropertyConfig globalPropertyConfig;

    @Autowired
    private NextGpuWebService nextGpuWebService;

    @Test
    void updateJwtProperty_shouldStoreJwtAndWallet() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.JWT_TOKEN))
                .thenReturn(Optional.empty());

        globalPropertyConfig.updateJwtProperty("0xABC");

        verify(globalPropertyRepository, atLeastOnce()).save(argThat(prop -> 
                prop.getName().equals(GlobalPropertyConfig.JWT_TOKEN) && 
                prop.getValueReference().equals("0xABC")));
    }

    @Test
    void updateLinuxDistroProperty_shouldCreateWhenMissing() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO))
                .thenReturn(Optional.empty());

        globalPropertyConfig.updateLinuxDistroProperty("ubuntu");

        verify(globalPropertyRepository).save(argThat(prop ->
                prop.getName().equals(GlobalPropertyConfig.LINUX_DISTRO)
                        && prop.getValueReference().equals("ubuntu")
        ));
    }

    @Test
    void updateLinuxDistroProperty_shouldUpdateWhenExists() {
        GlobalProperty existing = new GlobalProperty();
        existing.setName(GlobalPropertyConfig.LINUX_DISTRO);

        when(globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO))
                .thenReturn(Optional.of(existing));

        globalPropertyConfig.updateLinuxDistroProperty("debian");

        assertEquals("debian", existing.getValueReference());
        verify(globalPropertyRepository).save(existing);
    }

    @Test
    void updateOsUsernameProperty_shouldUpdateCorrectKey() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME))
                .thenReturn(Optional.empty());

        globalPropertyConfig.updateOsUsernameProperty("user1");

        verify(globalPropertyRepository).save(argThat(prop ->
                prop.getName().equals(GlobalPropertyConfig.OS_USERNAME)
                        && prop.getValueReference().equals("user1")
        ));
    }

    @Test
    void updateIsEulaAcceptedProperty_shouldUseCorrectPropertyKey() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.IS_EULA_ACCEPTED))
                .thenReturn(Optional.empty());

        globalPropertyConfig.updateIsEulaAcceptedProperty(true);

        verify(globalPropertyRepository).save(argThat(prop ->
                prop.getName().equals(GlobalPropertyConfig.IS_EULA_ACCEPTED)
                        && prop.getValueReference().equals("true")
        ));
    }

    @Test
    void updateIsSetupCompleteProperty_shouldUseCorrectPropertyKey() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.IS_SETUP_COMPLETED))
                .thenReturn(Optional.empty());

        globalPropertyConfig.updateIsSetupCompleteProperty(true);

        verify(globalPropertyRepository).save(argThat(prop ->
                prop.getName().equals(GlobalPropertyConfig.IS_SETUP_COMPLETED)
                        && prop.getValueReference().equals("true")
        ));
    }

    @Test
    void updateTokenHistoryLimitProperty_shouldPersistIntegerAsString() {
        when(globalPropertyRepository.findByName(GlobalPropertyConfig.TOKEN_HISTORY_LIMIT))
                .thenReturn(Optional.empty());

        globalPropertyConfig.updateTokenHistoryLimitProperty(5000);

        verify(globalPropertyRepository).save(argThat(prop ->
                prop.getName().equals(GlobalPropertyConfig.TOKEN_HISTORY_LIMIT)
                        && prop.getValueReference().equals("5000")
        ));
    }
}
