package ai.nextgpu.agent.util;

import ai.nextgpu.common.model.BaseComponent;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * This interface is implemented in hardware detection utilities
 */
public interface ComponentDetector {

    /**
     * Detects this component type on a Linux host by shelling out to the relevant system tools.
     *
     * @return the detected components, or an empty list if none were found
     * @throws IOException if a detection command could not be run or its output could not be read
     * @throws InterruptedException if the calling thread is interrupted while waiting for a detection command
     * @throws ParseException if a detection command's output could not be parsed
     */
    List<BaseComponent> detectLinux() throws IOException, InterruptedException, ParseException;

    /**
     * Detects this component type on a Windows host by shelling out to the relevant system tools.
     *
     * @return the detected components, or an empty list if none were found
     * @throws IOException if a detection command could not be run or its output could not be read
     * @throws InterruptedException if the calling thread is interrupted while waiting for a detection command
     * @throws ParseException if a detection command's output could not be parsed
     */
    List<BaseComponent> detectWindows() throws IOException, InterruptedException, ParseException;

    /**
     * Detects this component type on a macOS host by shelling out to the relevant system tools.
     *
     * @return the detected components, or an empty list if none were found
     * @throws IOException if a detection command could not be run or its output could not be read
     * @throws InterruptedException if the calling thread is interrupted while waiting for a detection command
     * @throws ParseException if a detection command's output could not be parsed
     */
    List<BaseComponent> detectMacOS() throws IOException, InterruptedException, ParseException;
}
