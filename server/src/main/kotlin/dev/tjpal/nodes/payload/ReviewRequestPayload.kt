package dev.tjpal.nodes.payload

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import kotlinx.serialization.Serializable

@Serializable
@JsonClassDescription(
    """
    Human-in-the-loop review request emitted by the LLM.

    Purpose:
    - Ask a human reviewer to evaluate an item produced by the LLM and provide a decision.
    
    Usage:
    - The human reviewer wil read the decisionDescription and either approve or deny the request.
    - When the human reviewer approves the request, "instructions" will be sent to a LLM for further processing.

    Example JSON:
    {
      "instructions": "Change the priority of the JIRA item PROJ-123 to 'Medium' and assign it to the 'Backend Team'.",
      "decisionDescription": "PROJ-123 reports *sporadic* failures when trying to log out. The recovery is a retry. Root cause is in the backend. Proposal: Set to 'Medium' and forward to Backend Team."
    }
    """
)
data class ReviewRequestPayload(
    @JsonPropertyDescription(
        """
        These instructions will be passed to a LLM to perform the actions you decided. 
        It should contain clear, specific and actionable instructions for the LLM to follow.
        Assume the LLM has all necessary tools to perform the action.
        For the human review this part is optional. Ensure it targets only the actions you want the LLM to perform upon approval.
        """
    )
    val instructions: String,
    @JsonPropertyDescription(
        """
        A clear, pertinent and apposite description of the decision the human reviewer must make. The human reviewer will either approve or decline the request.
        When approved, the "instructions" will be sent to a LLM for further processing. This description will NOT be sent to the LLM.
        Ensure it contains all relevant context for the human reviewer to make an informed decision.
        """
    )
    val decisionDescription: String
): NodePayload {
    override fun asString() = instructions
}