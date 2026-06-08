
package ai.nextgpu.common.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.nextgpu.common.dto.*;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.util.JsonUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Represents a hardware report that extends the base report functionality and provides detailed
 * information about the computer's hardware components. The HardwareReport class includes methods
 * to export the report to an HTML file with a structured, readable format.
 *
 * The report includes detailed information about the following types of hardware components:
 * - CPUs
 * - GPUs
 * - Memory modules
 * - Storages
 * - Network devices
 * - Other generic components
 *
 * It leverages Bootstrap for styling the HTML output.
 */
@Setter
@Entity
@Table(name = "hardware_report")
public class HardwareReport extends BaseReport {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hardware_report_seq_gen")
    @SequenceGenerator(name = "hardware_report_seq_gen", sequenceName = "hardware_report_seq", allocationSize = 1)
    private Long id;

    @Column(name = "computer_uuid", length = 38)
    @Setter
    @Getter
    private String computerUuid;

    @Lob
    @Column(name = "report_content", columnDefinition = "text")
    private String reportContent;

    public Computer getComputer(){
        if (reportContent != null && !reportContent.isBlank()) {
            try {
                return JsonUtil.OBJECT_MAPPER.readValue(reportContent, Computer.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return new Computer();
    }

    public void setComputer(Computer computer){
        try {
            computerUuid = computer.getUuid();
            reportContent = JsonUtil.OBJECT_MAPPER.writeValueAsString(computer);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Exports the hardware report to an HTML file at the specified path.
     * The report includes information about various hardware components,
     * such as CPUs, GPUs, memory modules, storage devices, network devices,
     * and other components, formatted with a Bootstrap-based layout.
     *
     * @param filename the path to the file where the HTML report will be exported
     *                 (e.g., "report.html"). This parameter should specify a valid file path.
     * @throws RuntimeException if an I/O error occurs while writing to the file
     */
    public void exportToHtml(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            Computer computer = getComputer();

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n")
                    .append("<html lang=\"en\">\n")
                    .append("<head>\n")
                    .append("    <meta charset=\"UTF-8\">\n")
                    .append("    <title>Hardware Report</title>\n")
                    .append("    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n")
                    .append("</head>\n")
                    .append("<body class=\"container mt-4\">\n")
                    .append("    <h1>Hardware Report</h1>\n")
                    .append("    <p class=\"text-muted\">Generated on: ")
                    .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append("</p>\n")
                    .append("    <div class=\"card mb-3\">\n")
                    .append("        <div class=\"card-header\"><h3>CPUs</h3></div>\n")
                    .append("        <div class=\"card-body\">")
                    .append(formatComponentList(computer.getCpus()))
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"card mb-3\">\n")
                    .append("        <div class=\"card-header\"><h3>GPUs</h3></div>\n")
                    .append("        <div class=\"card-body\">")
                    .append(formatComponentList(computer.getGpus()))
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"card mb-3\">\n")
                    .append("        <div class=\"card-header\"><h3>Memory</h3></div>\n")
                    .append("        <div class=\"card-body\">")
                    .append(formatComponentList(computer.getMemories()))
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"card mb-3\">\n")
                    .append("        <div class=\"card-header\"><h3>Storage</h3></div>\n")
                    .append("        <div class=\"card-body\">")
                    .append(formatComponentList(computer.getStorages()))
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"card mb-3\">\n")
                    .append("        <div class=\"card-header\"><h3>Network Devices</h3></div>\n")
                    .append("        <div class=\"card-body\">")
                    .append(formatComponentList(computer.getNetworkDevices()))
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"card mb-3\">\n")
                    .append("        <div class=\"card-header\"><h3>Other Components</h3></div>\n")
                    .append("        <div class=\"card-body\">")
                    .append(formatComponentList(computer.getOtherComponents()))
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("</body>\n")
                    .append("</html>");
            writer.write(html.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export HTML report: " + e.getMessage(), e);
        }
    }

    private String formatComponentList(Collection<?> components) {
        if (components == null || components.isEmpty()) {
            return "<p class=\"text-muted\">No components found</p>\n";
        }
        StringBuilder result = new StringBuilder("<ul class=\"list-group\">\n");
        for (Object component : components) {
            result.append("    <li class=\"list-group-item\">");

            if (component instanceof Cpu) {
                Cpu cpu = (Cpu) component;
                result.append("<div class=\"fw-bold\">").append(cpu.getManufacturer()).append(" ").append(cpu.getModel()).append("</div>");
                appendBaseComponentInfo(result, cpu);
                result.append("<div>Architecture: ").append(cpu.getArchitecture()).append("</div>");
                result.append("<div>Cores: ").append(cpu.getCores()).append(" (").append(cpu.getThreads()).append(" threads)</div>");
                result.append("<div>Clock: ").append(cpu.getMinClock()).append("-").append(cpu.getMaxClock()).append(" MHz</div>");
                if (cpu.getL3Cache() != null) {
                    result.append("<div>L3 Cache: ").append(cpu.getL3Cache()).append(" KB</div>");
                }
            } else if (component instanceof Gpu) {
                Gpu gpu = (Gpu) component;
                result.append("<div class=\"fw-bold\">").append(gpu.getManufacturer()).append(" ").append(gpu.getModel()).append("</div>");
                appendBaseComponentInfo(result, gpu);
                result.append("<div>Architecture: ").append(gpu.getArchitecture()).append("</div>");
                if (gpu.getShaderCores() != null) {
                    result.append("<div>Shader Cores: ").append(gpu.getShaderCores()).append("</div>");
                }
                if (gpu.getTensorCores() != null) {
                    result.append("<div>Tensor Cores: ").append(gpu.getTensorCores()).append("</div>");
                }
                result.append("<div>Clock: ").append(gpu.getMinClock()).append("-").append(gpu.getMaxClock()).append(" MHz</div>");
                result.append("<div>Memory: ").append(gpu.getCapacity()).append(" GB ").append(gpu.getType()).append("</div>");
            } else if (component instanceof MemoryModule) {
                MemoryModule memory = (MemoryModule) component;
                result.append("<div class=\"fw-bold\">").append(memory.getManufacturer()).append(" ").append(memory.getModel()).append("</div>");
                appendBaseComponentInfo(result, memory);
                result.append("<div>Type: ").append(memory.getType()).append("</div>");
                result.append("<div>Capacity: ").append(memory.getCapacity()).append(" MB</div>");
                result.append("<div>Speed: ").append(memory.getBusSpeed()).append(" MHz</div>");
            } else if (component instanceof Storage) {
                Storage storage = (Storage) component;
                result.append("<div class=\"fw-bold\">").append(storage.getManufacturer()).append(" ").append(storage.getModel()).append("</div>");
                appendBaseComponentInfo(result, storage);
                result.append("<div>Type: ").append(storage.getType()).append("</div>");
                result.append("<div>Capacity: ").append(storage.getCapacity()).append(" GB</div>");
                if (storage.getCache() != null) {
                    result.append("<div>Cache: ").append(storage.getCache()).append(" MB</div>");
                }
            } else if (component instanceof NetworkDevice) {
                NetworkDevice networkDevice = (NetworkDevice) component;
                result.append("<div class=\"fw-bold\">").append(networkDevice.getManufacturer()).append(" ").append(networkDevice.getModel()).append("</div>");
                appendBaseComponentInfo(result, networkDevice);
                result.append("<div>MAC Address: ").append(networkDevice.getMacAddress()).append("</div>");
                result.append("<div>Speed: ").append(networkDevice.getSpeed()).append(" Mbps</div>");
            } else if (component instanceof GenericComponent) {
                GenericComponent genericComponent = (GenericComponent) component;
                result.append("<div class=\"fw-bold\">").append(genericComponent.getManufacturer()).append(" ").append(genericComponent.getModel()).append("</div>");
                appendBaseComponentInfo(result, genericComponent);
                result.append("<div>Type: ").append(genericComponent.getType()).append("</div>");
                result.append("<div>").append(genericComponent.getSpecificationKey()).append(": ").append(genericComponent.getSpecificationValue()).append("</div>");
            } else {
                result.append(component.toString());
            }

            result.append("</li>\n");
        }
        result.append("</ul>\n");
        return result.toString();
    }

    private void appendBaseComponentInfo(StringBuilder result, BaseComponent component) {
        if (component.getYearReleased() != null) {
            result.append("<div>Year Released: ").append(component.getYearReleased()).append("</div>");
        }
        if (component.getProductIdentifier() != null && !component.getProductIdentifier().isEmpty()) {
            result.append("<div>Product ID: ").append(component.getProductIdentifier()).append("</div>");
        }
        if (component.getIsDiscontinued() != null && component.getIsDiscontinued()) {
            result.append("<div class=\"text-danger\">Discontinued</div>");
        }
        if (component.getTdpWatts() != null) {
            result.append("<div>TDP: ").append(component.getTdpWatts()).append(" W</div>");
        }
    }

    public void exportToPdf(String filename) {
        //TODO: Generate the HTML report and use some trick to export as PDF as-is
        //  for now, we can suggest the user to populate the HTML file and print it as a PDF on browser
    }

    /**
     * Exports the hardware report to a plain text file at the specified path.
     * The report includes information about various hardware components,
     * such as CPUs, GPUs, memory modules, storage devices, network devices,
     * and other components, formatted with indentation for readability.
     *
     * @param filename the path to the file where the text report will be exported
     *                 (e.g., "report.txt"). This parameter should specify a valid file path.
     * @throws RuntimeException if an I/O error occurs while writing to the file
     */
    public void exportToText(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            Computer computer = getComputer();

            StringBuilder text = new StringBuilder();
            text.append("Hardware Report\n");
            text.append("==============\n\n");
            text.append("Generated on: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

            // CPUs
            text.append("CPUs\n");
            text.append("----\n");
            text.append(formatComponentListAsText(computer.getCpus())).append("\n");

            // GPUs
            text.append("GPUs\n");
            text.append("----\n");
            text.append(formatComponentListAsText(computer.getGpus())).append("\n");

            // Memory
            text.append("Memory\n");
            text.append("------\n");
            text.append(formatComponentListAsText(computer.getMemories())).append("\n");

            // Storage
            text.append("Storage\n");
            text.append("-------\n");
            text.append(formatComponentListAsText(computer.getStorages())).append("\n");

            // Network Devices
            text.append("Network Devices\n");
            text.append("---------------\n");
            text.append(formatComponentListAsText(computer.getNetworkDevices())).append("\n");

            // Other Components
            text.append("Other Components\n");
            text.append("----------------\n");
            text.append(formatComponentListAsText(computer.getOtherComponents()));

            writer.write(text.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export text report: " + e.getMessage(), e);
        }
    }

    private String formatComponentListAsText(Collection<?> components) {
        if (components == null || components.isEmpty()) {
            return "  No components found\n";
        }

        StringBuilder result = new StringBuilder();
        for (Object component : components) {
            if (component instanceof Cpu) {
                Cpu cpu = (Cpu) component;
                result.append("  ").append(cpu.getManufacturer()).append(" ").append(cpu.getModel()).append("\n");
                appendBaseComponentInfoAsText(result, cpu);
                result.append("    Architecture: ").append(cpu.getArchitecture()).append("\n");
                result.append("    Cores: ").append(cpu.getCores()).append(" (").append(cpu.getThreads()).append(" threads)\n");
                result.append("    Clock: ").append(cpu.getMinClock()).append("-").append(cpu.getMaxClock()).append(" MHz\n");
                if (cpu.getL3Cache() != null) {
                    result.append("    L3 Cache: ").append(cpu.getL3Cache()).append(" KB\n");
                }
            } else if (component instanceof Gpu) {
                Gpu gpu = (Gpu) component;
                result.append("  ").append(gpu.getManufacturer()).append(" ").append(gpu.getModel()).append("\n");
                appendBaseComponentInfoAsText(result, gpu);
                result.append("    Architecture: ").append(gpu.getArchitecture()).append("\n");
                if (gpu.getShaderCores() != null) {
                    result.append("    Shader Cores: ").append(gpu.getShaderCores()).append("\n");
                }
                if (gpu.getTensorCores() != null) {
                    result.append("    Tensor Cores: ").append(gpu.getTensorCores()).append("\n");
                }
                result.append("    Clock: ").append(gpu.getMinClock()).append("-").append(gpu.getMaxClock()).append(" MHz\n");
                result.append("    Memory: ").append(gpu.getCapacity()).append(" GB ").append(gpu.getType()).append("\n");
            } else if (component instanceof MemoryModule) {
                MemoryModule memory = (MemoryModule) component;
                result.append("  ").append(memory.getManufacturer()).append(" ").append(memory.getModel()).append("\n");
                appendBaseComponentInfoAsText(result, memory);
                result.append("    Type: ").append(memory.getType()).append("\n");
                result.append("    Capacity: ").append(memory.getCapacity()).append(" MB\n");
                result.append("    Speed: ").append(memory.getBusSpeed()).append(" MHz\n");
            } else if (component instanceof Storage) {
                Storage storage = (Storage) component;
                result.append("  ").append(storage.getManufacturer()).append(" ").append(storage.getModel()).append("\n");
                appendBaseComponentInfoAsText(result, storage);
                result.append("    Type: ").append(storage.getType()).append("\n");
                result.append("    Capacity: ").append(storage.getCapacity()).append(" GB\n");
                if (storage.getCache() != null) {
                    result.append("    Cache: ").append(storage.getCache()).append(" MB\n");
                }
            } else if (component instanceof NetworkDevice) {
                NetworkDevice networkDevice = (NetworkDevice) component;
                result.append("  ").append(networkDevice.getManufacturer()).append(" ").append(networkDevice.getModel()).append("\n");
                appendBaseComponentInfoAsText(result, networkDevice);
                result.append("    MAC Address: ").append(networkDevice.getMacAddress()).append("\n");
                result.append("    Speed: ").append(networkDevice.getSpeed()).append(" Mbps\n");
            } else if (component instanceof GenericComponent) {
                GenericComponent genericComponent = (GenericComponent) component;
                result.append("  ").append(genericComponent.getManufacturer()).append(" ").append(genericComponent.getModel()).append("\n");
                appendBaseComponentInfoAsText(result, genericComponent);
                result.append("    Type: ").append(genericComponent.getType()).append("\n");
                result.append("    ").append(genericComponent.getSpecificationKey()).append(": ").append(genericComponent.getSpecificationValue()).append("\n");
            } else {
                result.append("  ").append(component.toString()).append("\n");
            }
            result.append("\n");
        }
        return result.toString();
    }

    private void appendBaseComponentInfoAsText(StringBuilder result, BaseComponent component) {
        if (component.getYearReleased() != null) {
            result.append("    Year Released: ").append(component.getYearReleased()).append("\n");
        }
        if (component.getProductIdentifier() != null && !component.getProductIdentifier().isEmpty()) {
            result.append("    Product ID: ").append(component.getProductIdentifier()).append("\n");
        }
        if (component.getIsDiscontinued() != null && component.getIsDiscontinued()) {
            result.append("    Discontinued\n");
        }
        if (component.getTdpWatts() != null) {
            result.append("    TDP: ").append(component.getTdpWatts()).append(" W\n");
        }
    }

    @Override
    public JsonNode asJson() {
        Computer computer = getComputer();

        return new ObjectMapper().createObjectNode()
                .put("computerId", computerUuid)
                .putPOJO("cpus", computer.getCpus().stream().map(CpuDto::toDto).collect(Collectors.toList()))
                .putPOJO("gpus", computer.getGpus().stream().map(GpuDto::toDto).collect(Collectors.toList()))
                .putPOJO("memories", computer.getMemories().stream().map(MemoryModuleDto::toDto).collect(Collectors.toList()))
                .putPOJO("storages", computer.getStorages().stream().map(StorageDto::toDto).collect(Collectors.toList()))
                .putPOJO("operatingSystems", computer.getOperatingSystem())
                .putPOJO("networkDevices", computer.getNetworkDevices().stream().map(NetworkDeviceDto::toDto).collect(Collectors.toList()))
                .putPOJO("otherComponents", computer.getOtherComponents().stream().map(GenericComponentDto::toDto).collect(Collectors.toList()));
    }
}
