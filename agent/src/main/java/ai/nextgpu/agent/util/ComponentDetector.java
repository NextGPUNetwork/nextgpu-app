package ai.nextgpu.agent.util;

import ai.nextgpu.common.model.BaseComponent;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * This interface is implemented in hardware detection utilities
 */
public interface ComponentDetector {

    List<BaseComponent> detectLinux() throws IOException, InterruptedException, ParseException;

    List<BaseComponent> detectWindows() throws IOException, InterruptedException, ParseException;

    List<BaseComponent> detectMacOS() throws IOException, InterruptedException, ParseException;
}
