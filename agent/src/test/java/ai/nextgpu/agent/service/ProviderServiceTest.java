package ai.nextgpu.agent.service;

import ai.nextgpu.agent.repository.ProviderAttributeTypeRepository;
import ai.nextgpu.agent.repository.ProviderRepository;
import ai.nextgpu.common.dto.ProviderAttributeTypeDto;
import ai.nextgpu.common.dto.UserDto;
import ai.nextgpu.common.model.Provider;
import ai.nextgpu.common.model.ProviderAttributeType;
import ai.nextgpu.common.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderAttributeTypeRepository providerAttributeTypeRepository;

    @Mock
    private ProviderRepository providerRepository;

    private ProviderService providerService;

    @BeforeEach
    void setUp() {
        providerService = new ProviderService(providerAttributeTypeRepository, providerRepository);
    }

    @Test
    void saveProvider_NewProvider_SavesSuccessfully() {
        UserDto userDto = new UserDto();
        userDto.setWalletAddress("0x123");
        userDto.setUuid("uuid-1");
        userDto.setRole(Role.PROVIDER);
        userDto.setName("Test Provider");
        userDto.setEmail("test@example.com");
        userDto.setCity("London");
        userDto.setCountry("UK");
        userDto.setAttributes(new HashMap<>());

        when(providerRepository.findByWalletAddress("0x123")).thenReturn(Optional.empty());

        Provider result = providerService.saveProvider(userDto);

        assertNotNull(result);
        assertEquals("0x123", result.getWalletAddress());
        assertEquals("uuid-1", result.getUuid());
        assertEquals(Role.PROVIDER, result.getRole());
        assertEquals("Test Provider", result.getName());
        assertEquals("test@example.com", result.getProviderEmail());
        assertEquals("London", result.getCity());
        assertEquals("UK", result.getCountry());
        verify(providerRepository).save(any(Provider.class));
    }

    @Test
    void saveProvider_ExistingProvider_UpdatesAndSavesSuccessfully() {
        UserDto userDto = new UserDto();
        userDto.setWalletAddress("0x123");
        userDto.setUuid("uuid-updated");
        userDto.setName("Updated Name");

        Provider existingProvider = new Provider();
        existingProvider.setWalletAddress("0x123");
        existingProvider.setName("Old Name");

        when(providerRepository.findByWalletAddress("0x123")).thenReturn(Optional.of(existingProvider));

        Provider result = providerService.saveProvider(userDto);

        assertNotNull(result);
        assertEquals("uuid-updated", result.getUuid());
        assertEquals("Updated Name", result.getName());
        verify(providerRepository).save(existingProvider);
    }

    @Test
    void saveProvider_WithAttributes_MapsAttributesCorrectly() {
        UserDto userDto = new UserDto();
        userDto.setWalletAddress("0x123");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("gpu_model", "RTX 3080");
        userDto.setAttributes(attributes);

        ProviderAttributeType attributeType = new ProviderAttributeType();
        attributeType.setName("gpu_model");

        when(providerRepository.findByWalletAddress("0x123")).thenReturn(Optional.empty());
        when(providerAttributeTypeRepository.findByName("gpu_model")).thenReturn(Optional.of(attributeType));

        Provider result = providerService.saveProvider(userDto);

        assertNotNull(result.getProviderAttributes());
        assertEquals(1, result.getProviderAttributes().size());
        assertEquals("RTX 3080", result.getProviderAttributes().get(attributeType));
    }

    @Test
    void saveProvider_withNullAttributes_doesNotFail() {
        UserDto userDto = new UserDto();
        userDto.setWalletAddress("0x123");
        userDto.setAttributes(null);

        when(providerRepository.findByWalletAddress("0x123")).thenReturn(Optional.empty());

        Provider result = providerService.saveProvider(userDto);

        assertNotNull(result.getProviderAttributes());
        assertTrue(result.getProviderAttributes().isEmpty());
    }

    @Test
    void saveProvider_withUnknownAttributeType_skipsIt() {
        UserDto userDto = new UserDto();
        userDto.setWalletAddress("0x123");
        userDto.setAttributes(Map.of("unknown", "value"));

        when(providerRepository.findByWalletAddress("0x123")).thenReturn(Optional.empty());
        when(providerAttributeTypeRepository.findByName("unknown")).thenReturn(Optional.empty());

        Provider result = providerService.saveProvider(userDto);

        assertTrue(result.getProviderAttributes().isEmpty());
    }

    @Test
    void getProviderAttributeTypeByName_Found_ReturnsAttributeType() {
        ProviderAttributeType attributeType = new ProviderAttributeType();
        attributeType.setName("test");
        when(providerAttributeTypeRepository.findByName("test")).thenReturn(Optional.of(attributeType));

        ProviderAttributeType result = providerService.getProviderAttributeTypeByName("test");

        assertNotNull(result);
        assertEquals("test", result.getName());
    }

    @Test
    void getProviderAttributeTypeByName_NotFound_ThrowsException() {
        when(providerAttributeTypeRepository.findByName("unknown")).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class, () -> providerService.getProviderAttributeTypeByName("unknown"));
    }

    @Test
    void getAllProviderAttributeTypes_ReturnsList() {
        List<ProviderAttributeType> list = Collections.singletonList(new ProviderAttributeType());
        when(providerAttributeTypeRepository.findAll()).thenReturn(list);

        List<ProviderAttributeType> result = providerService.getAllProviderAttributeTypes();

        assertEquals(list, result);
    }

    @Test
    void saveProviderAttributeTypes_CallsRepository() {
        ProviderAttributeTypeDto dto = new ProviderAttributeTypeDto();
        dto.setName("gpu_model");
        dto.setDescription("GPU model");
        dto.setDatatype("java.lang.String");
        dto.setIsMandatory(true);
        dto.setIsUnique(false);
        dto.setValidationRegex(".*");

        List<ProviderAttributeTypeDto> list = Collections.singletonList(dto);

        providerService.saveProviderAttributeTypes(list);

        verify(providerAttributeTypeRepository).save(argThat(entity ->
                entity != null
                        && "gpu_model".equals(entity.getName())
                        && "GPU model".equals(entity.getDescription())
                        && "java.lang.String".equals(entity.getDatatype())
                        && Boolean.TRUE.equals(entity.getIsMandatory())
                        && Boolean.FALSE.equals(entity.getIsUnique())
                        && ".*".equals(entity.getValidationRegex())
        ));
    }
}
