import shutil
import time
import json
import subprocess
import urllib
import nbformat
import papermill as pm
from pathlib import Path

class NotebookService:

    USERNAME = "" 
    NOTEBOOK_NAME = ""
    NOTEBOOK_FILE = ""
    WORKDIR = ""
    METADATA_PATH = ""


    def __init__(self, username, notebook_name):
        self.USERNAME = username
        self.NOTEBOOK_NAME = notebook_name
        self.NOTEBOOK_FILE = f"{self.NOTEBOOK_NAME}.ipynb"
        self.WORKDIR = Path(f"./kaggle_notebook_{self.NOTEBOOK_NAME}")
        self.METADATA_PATH = self.WORKDIR / "kernel-metadata.json"
        self.WORKDIR.mkdir(exist_ok=True)


    """
    
    Create a new notebook or append a new cell to the existing notebook

    """
    def create_or_append_notebook(self, cell):
        notebook_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb"
        backup_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}-backup.ipynb"
        
        notebook_str = self.read_notebook(self.NOTEBOOK_NAME)
        notebook = None

        if notebook_str is not None:
            notebook = notebook_str

            # Backup before modifying
            shutil.copy(notebook_path, backup_path)
            print(f"Backup created at {backup_path}")

        cell_str = urllib.parse.unquote(cell)
        cell = json.loads(cell_str)

        if notebook is not None:
            # Append a cell (cell must be parsed from JSON string to dict if needed)
            notebook["cells"].append(cell)
            self.write_to_notebook(notebook, self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb")
            print(f"Appended cell to notebook {self.NOTEBOOK_NAME}")
        else:
            # Create new notebook from full JSON content
            notebook = cell
            self.write_to_notebook(notebook, self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb")
            print(f"Created new notebook {self.NOTEBOOK_NAME}")


    """
    
    Test the notebook execution using papermill

    """
    def test_notebook(self):
        notebook_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb"
        notebook_output_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}-output.ipynb"
        backup_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}-backup.ipynb"

        try:
            pm.execute_notebook(notebook_path, notebook_output_path)
        except Exception as e:
            print("An error occurred while executing the notebook:", e)
            self.revert_notebook()
            return e
        
        print("Notebook service is operational.")

        # Remove backup if execution was successful
        if backup_path.exists():
            backup_path.unlink()  
            print(f"Removed backup at {backup_path}")

        return self.get_last_notebook_output(notebook_output_path)


    """
    
    Push the notebook to Kaggle and monitor its status
    
    """
    def push_to_kaggle(self):
        self.create_metadata()
        
        try:
            subprocess.run(["kaggle", "kernels", "push", "-p", str(self.WORKDIR)], check=True)

            # Poll until finished
            while True:
                result = subprocess.run(
                    ["kaggle", "kernels", "status", f"{self.USERNAME}/{self.NOTEBOOK_NAME}"],
                    capture_output=True, text=True
                )

                status_output = result.stdout.strip()
                print("Status:", status_output)

                if "complete" in status_output.lower():
                    print("Notebook finished successfully.")
                    break
                elif "error" in status_output.lower() or "failed" in status_output.lower():
                    print("Notebook failed.")
                    break

                time.sleep(20)  # wait 20 seconds before checking again

            # Download outputs once complete
            subprocess.run(
                ["kaggle", "kernels", "output", f"{self.USERNAME}/{self.NOTEBOOK_NAME}", "-p", "outputs"],
                check=True
            )

            print("Pushed notebook to Kaggle.") 
        except subprocess.CalledProcessError as e:
            print("An error occurred while pushing to Kaggle:", e)
            return None


    """

    Read an existing notebook file and load its content 
    
    """
    def read_notebook(self, notebook_name):
        notebook_path = self.WORKDIR / f"{notebook_name}.ipynb"
        notebook = None

        try:
            # Load the notebook as a Python dictionary
            with open(notebook_path, "r", encoding="utf-8") as f:
                notebook = json.load(f)
        except Exception as e:
            print("An error occurred while reading the notebook:", e)
            return None

        return notebook


    """

    Write the current notebook content to a file

    """
    def write_to_notebook(self, notebook, notebook_path):
        print("Writing to notebook:", notebook)
        try:
            with open(notebook_path, "w") as f:
                json.dump(notebook, f, indent=2)
            print(f"Wrote to notebook at {notebook_path}")
        except Exception as e:
            print("An error occurred while writing to the notebook:", e)
            return None


    """
    
    Revert the notebook to the last backup or delete if no backup exists
    
    """
    def revert_notebook(self):
        notebook_path = self.WORKDIR / self.NOTEBOOK_FILE
        backup_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}-backup.ipynb"

        try:
            if backup_path.exists():
                shutil.copy(backup_path, notebook_path)
                backup_path.unlink()
                print(f"Reverted notebook to backup from {backup_path}")
            else:
                print("No backup found to revert to. Deleting notebook")
                if notebook_path.exists():
                    notebook_path.unlink()
        except Exception as e:
            print("An error occurred while reverting the notebook:", e)
            return None

    def get_last_notebook_output(self, notebook_output_path):
        try:
            # Load the executed notebook
            nb = nbformat.read(notebook_output_path, as_version=4)
        except Exception as e:
            print("An error occurred while reading the executed notebook:", e)
            return ""

        # Get the last code cell that has output
        last_output = None
        for cell in reversed(nb.cells):
            if cell.cell_type == 'code' and 'outputs' in cell and len(cell.outputs) > 0:
                # Get the last output from the last code cell
                last_output = cell.outputs[-1]
                break

        if last_output is not None:
            # If it's a stream output (like print statements)
            if last_output.output_type == 'stream':
                print(last_output.text)
                return last_output.text
            # If it's display data (like matplotlib, pandas HTML)
            elif last_output.output_type in ['display_data', 'execute_result']:
                print(last_output.data)
                return last_output.data
        else:
            print("No output found in the notebook.")
            return ""


    """
    
    Create the kernel-metadata.json file required by Kaggle
    
    """
    def create_metadata(self):
        kernel_metadata = {
            "id": f"{self.USERNAME}/{self.NOTEBOOK_NAME}",
            "title": self.NOTEBOOK_NAME,
            "code_file": self.NOTEBOOK_FILE,
            "language": "python",
            "kernel_type": "notebook",
            "is_private": True,
            "enable_gpu": True,
            "enable_internet": False
        }
        
        try:
            with open(self.METADATA_PATH, "w") as f:
                json.dump(kernel_metadata, f, indent=2)
            print(f"Created metadata at {self.METADATA_PATH}")
        except Exception as e:
            print("An error occurred while creating metadata:", e)
            return None
