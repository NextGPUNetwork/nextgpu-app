package ai.nextgpu.agent.service;

import ai.nextgpu.agent.repository.ProviderAttributeTypeRepository;
import ai.nextgpu.agent.repository.ProviderRepository;
import ai.nextgpu.common.dto.ProviderAttributeTypeDto;
import ai.nextgpu.common.dto.UserDto;
import ai.nextgpu.common.model.Provider;
import ai.nextgpu.common.model.ProviderAttributeType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProviderService {
    private static final Logger log = LoggerFactory.getLogger(ProviderService.class);
    private final ProviderAttributeTypeRepository providerAttributeTypeRepository;
    private final ProviderRepository providerRepository;


    public ProviderService(ProviderAttributeTypeRepository providerAttributeTypeRepository, ProviderRepository providerRepository) {
        this.providerAttributeTypeRepository = providerAttributeTypeRepository;
        this.providerRepository = providerRepository;
    }

    @Transactional
    public Provider saveProvider(UserDto userDto) {
        log.debug("Saving provider: {}", userDto);
        Provider provider = providerRepository.findByWalletAddress(userDto.getWalletAddress())
                .orElseGet(Provider::new);
        provider.setUuid(userDto.getUuid());
        provider.setUsername(userDto.getUsername() != null ? userDto.getUsername() : userDto.getWalletAddress());
        provider.setRole(userDto.getRole());
        provider.setName(userDto.getName());
        provider.setWalletAddress(userDto.getWalletAddress());
        provider.setProviderEmail(userDto.getEmail());
        provider.setCity(userDto.getCity());
        provider.setCountry(userDto.getCountry());
        Map<ProviderAttributeType, String> providerAttributes = getProviderAttributeTypeStringMap(userDto);
        provider.setProviderAttributes(providerAttributes);
        providerRepository.save(provider);
        return provider;
    }

    public Provider getProviderByWalletAddress(String walletAddress) {
        return providerRepository.findByWalletAddress(walletAddress).orElse(null);
    }

    public Provider getProviderByUuid(String uuid) {
        return providerRepository.findByUuid(uuid).orElse(null);
    }

    //*******************************//
    // ** Provider Attribute Type ** //
    //*******************************//

    public ProviderAttributeType getProviderAttributeTypeByName(String name) {
        return providerAttributeTypeRepository.findByName(name).orElseThrow(
                () -> new NullPointerException("Provider attribute type with name " + name + " not found")
        );
    }

    public List<ProviderAttributeType> getAllProviderAttributeTypes() {
        return providerAttributeTypeRepository.findAll();
    }

    public void saveProviderAttributeTypes(List<ProviderAttributeTypeDto> providerAttributeTypes) {
        log.debug("Saving provider attribute types: {}", providerAttributeTypes);
        for (ProviderAttributeTypeDto dto : providerAttributeTypes) {
            ProviderAttributeType attributeType = providerAttributeTypeRepository
                    .findByUuid(dto.getUuid())
                    .orElseGet(ProviderAttributeType::new);

            attributeType.setUuid(dto.getUuid());
            attributeType.setName(dto.getName());
            attributeType.setDescription(dto.getDescription());
            attributeType.setDatatype(dto.getDatatype());
            attributeType.setIsMandatory(dto.getIsMandatory());
            attributeType.setIsUnique(dto.getIsUnique());
            attributeType.setValidationRegex(dto.getValidationRegex());

            providerAttributeTypeRepository.save(attributeType);
        }
    }

    private @NotNull Map<ProviderAttributeType, String> getProviderAttributeTypeStringMap(UserDto userDto) {
        Map<ProviderAttributeType, String> providerAttributes = new HashMap<>();
        Map<String, String> userAttributes = userDto.getAttributes();

        if (userAttributes != null && !userAttributes.isEmpty()) {
            userAttributes.forEach((attributeTypeName, attributeValue) -> {
                if (attributeTypeName != null && !attributeTypeName.isBlank()) {
                    providerAttributeTypeRepository.findByName(attributeTypeName.trim())
                            .ifPresent(attributeType -> providerAttributes.put(attributeType, attributeValue));
                }
            });
        }
        return providerAttributes;
    }
}
