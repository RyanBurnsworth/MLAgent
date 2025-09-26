import json
from fastapi import FastAPI
from services.dataset_service import DatasetService
import papermill as pm

app = FastAPI()

@app.get("/dataset/download/{search_term}")
def download_dataset(search_term: str):
    dataset_service = DatasetService()

    result = dataset_service.download_dataset(search_term)

    return result

@app.post("/notebook/test/{notebook_name}")
def test_notebook(notebook_name: str, notebook_content: str):
    from services.notebook_service import NotebookService
    notebook_service = NotebookService("ryanburnsworth", notebook_name)

    notebook_service.create_or_append_notebook(notebook_content)

    result = notebook_service.test_notebook()
    if result is Exception:
        return {"status": "error", "message": str(result)}
    return {"status": "success", "message": json.loads(result)}
