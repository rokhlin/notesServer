# Rule: Task Initialization & Documentation Inspection

## Mandatory Context Ingestion
Whenever a new task is formulated or initiated:
1. **Inspect and Read `docs/`**: The AI agent MUST proactively check the `docs/` folder and read relevant documentation files before planning, modifying code, or executing skills.
2. **Context Alignment**: Verify that proposed solutions align with existing architectural guidelines, specifications, and constraints recorded in `docs/`.
3. **Consistency**: Ensure any new endpoints, database updates, or logic changes stay in sync with the documentation, and update `docs/` if specifications evolve.
