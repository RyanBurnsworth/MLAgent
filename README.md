# Machine Learning Agent

Machine Learning Agent is an **agentic system** that automatically generates **end-to-end machine learning notebooks** from datasets.  
It is built using **Java (Spring AI)** and **Python (FastAPI)**, with seamless integration to **Kaggle’s API** for dataset retrieval.

---

## 🚀 Key Capabilities

| Capability | Description |
|------------|-------------|
| 🔽 Dataset Acquisition | Automatically downloads and verifies datasets (via Kaggle API or direct URLs) |
| 🧹 Data Preprocessing | Handles cleaning, transformation, and feature processing |
| 🧠 Model Training | Trains one or more ML models using the generated notebook |
| 📊 Performance Evaluation | Produces visual accuracy metrics (plots, confusion matrices, etc.) |

Two core agents drive the notebook lifecycle:

| Agent | Responsibility |
|--------|----------------|
| **NotebookCreatorAgent** | Builds a machine learning notebook from scratch |
| **NotebookUpdaterAgent** | Revises or enhances an existing notebook |

---

## 🔄 Self-Correcting + Self-Healing Architecture

Each step in notebook creation is **validated before execution**.

### ✅ Self-Correction Loop

| Component | Role |
|-----------|------|
| **CriticAgent** | Reviews generated code before execution |
| **EvaluatorAgent** | Provides structured feedback when changes are required |

Workflow:

1. Agent generates code for a step.
2. CriticAgent reviews it:
   - If **approved** ➝ code is executed
   - If **rejected** ➝ feedback is applied and code is regenerated
3. This repeats **up to 3 correction attempts**, after which the **last revision is accepted**

---

### 🛠️ Self-Healing Runtime

After execution, the notebook is **tested using Papermill**.  
If an error occurs:

| Component | Role |
|-----------|------|
| **ErrorHandlerAgent** | Receives the exception, traceback, and failing code |
| | Attempts to automatically fix issues **up to 3 times** |
| | If unresolved ➝ returns **HTTP 500 with diagnostic messaging** |

---

## 🧱 Tech Stack

| Layer | Technology |
|--------|-----------|
| **Core Orchestration** | Java + Spring AI |
| **Execution & Notebook Manipulation** | Python + FastAPI |
| **Dataset Integration** | Kaggle API |
| **Notebook Validation** | Papermill |

---
## 📌 Roadmap

- [ ] Multi-model comparison support (e.g., RandomForest vs XGBoost vs NeuralNet)
- [ ] Notebook-to-Production pipeline export (FastAPI endpoint / Batch job)
- [ ] Support for SQL / API-based data sources (Snowflake, BigQuery, REST, etc.)
- [ ] LLM-based summary of results and insights section at the end of notebooks

