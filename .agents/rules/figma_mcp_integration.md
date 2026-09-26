# Rule: Figma MCP Integration and Design-to-Code Workflow

## 1. Connection and Context Initialization
Whenever a task in `features.md` involves UI/UX implementation or architectural flow analysis, the AI must establish a connection to the project's Figma workspace via the **Figma MCP Server**.
- **Authorization:** The AI must securely access the Figma API using credentials stored exclusively in the project's `.env` file (e.g., `FIGMA_PERSONAL_ACCESS_TOKEN` and `FIGMA_FILE_KEY`). These credentials must never be hardcoded, committed to version control, or logged.
- **Context Extraction:** Before writing any UI code, the AI must query the Figma MCP to read the relevant frames, components, and design tokens (colors, typography, spacing).

## 2. Design-to-Code Implementation (Compose Multiplatform)
When translating Figma layouts into code, the AI must adhere to a strict Component-Driven Development pipeline:
- **Atomic Generation First:** The AI must focus on generating single, reusable `@Composable` UI components (e.g., buttons, input fields, cards, list items) by analyzing Figma's Main Components and Variants.
- **Page Composition:** Once the foundational components are created, the AI must connect and reuse them to construct the full page layouts (screens).
- **Semantic Translation:** Figma Auto Layout properties must be semantically translated into Compose Multiplatform equivalents (`Column`, `Row`, `Box`, `Arrangement`, `Alignment`, `Modifier.padding`, `Modifier.weight`). Absolute positioning is strictly prohibited unless explicitly mandated by the design.
- **Design Tokens:** The AI must extract local variables and styles from Figma and map them directly to the project's Compose `MaterialTheme` or custom theme system. Hardcoded hex colors, SP, or DP values are prohibited if a corresponding token exists in Figma.

## 3. Architectural and Flow Analysis
- The AI must use Figma prototyping links and connector lines (extracted via MCP) to map out user journeys.
- This mapping must be used to validate and update the `system_architecture.md` file, ensuring that all UI states (loading, error, empty) and screen transitions designed in Figma have a corresponding programmatic implementation in the architecture document.