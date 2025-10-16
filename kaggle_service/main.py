import json
from fastapi import FastAPI
from services.dataset_service import DatasetService

app = FastAPI()

@app.get("/dataset/download/{search_term}")
def download_dataset(search_term: str):
    dataset_service = DatasetService()

    result = dataset_service.download_dataset(search_term)
    if result is None:
        return {"status": "error", "message": "Dataset not found or an error occurred."}

    return result

@app.post("/notebook/update/{notebook_name}")
def update_notebook(notebook_name: str, notebook_content: str):
    from services.notebook_service import NotebookService
    notebook_service = NotebookService("ryanburnsworth", notebook_name)

    is_complete = notebook_service.create_or_append_notebook(notebook_content)
    if not is_complete:
        return {"status": "error", "message": "Failed to create or append to notebook."}

    result = notebook_service.test_notebook()
    if result is Exception:
        return {"status": "error", "message": str(result)}

    return {"status": "success", "message": json.loads(result)}
