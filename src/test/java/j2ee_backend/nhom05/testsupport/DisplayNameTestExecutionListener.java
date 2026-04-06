package j2ee_backend.nhom05.testsupport;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public class DisplayNameTestExecutionListener implements TestExecutionListener {

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            System.out.println("[TEST START] " + testIdentifier.getDisplayName());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            System.out.println("[TEST END] " + testIdentifier.getDisplayName() + " -> " + testExecutionResult.getStatus());
        }
    }
}