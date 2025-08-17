package com.culture.chatbot.chat;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class HcmSupportTool {

    @Tool(name = "updateEmployeeIdInstructions", description = "Provides a comprehensive, multi-step guide for a technical support specialist to update an employee's ID in the backend system. The instructions are in Markdown format and include all required database scripts, commands, and approval steps. This tool requires the specific employee's old ID (e.g., '12345'), employee's new ID (e.g., '12345'), the employee's current work agreement ID (e.g., '12345N'), and the employee's company id (e.g., '12345') that needs to be updated.")
    public String updateEmployeeId(String newEmployeeId, String oldEmployeeId, String companyId, String workAgreementId) {
        try {
            String markdownContent = new String(Files.readAllBytes(Paths.get("src/main/resources/data/how-to-update-ee-id.md")));
            return markdownContent.replace("{{NEW_EE_ID}}", newEmployeeId)
                    .replace("{{OLD_EE_ID}}", oldEmployeeId)
                    .replace("{{COMPANY_ID}}", companyId)
                    .replace("{{WORK_AGREEMENT_ID}}", workAgreementId);
        } catch (IOException e) {
            return "Error: Could not retrieve instructions for updating employee ID " + oldEmployeeId + ".";
        }
    }
}