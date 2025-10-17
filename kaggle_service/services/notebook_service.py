import shutil
import time
import json
import subprocess
import traceback
import papermill as pm
from pathlib import Path

class NotebookService:

    def __init__(self, username, notebook_name):
        self.USERNAME = username
        self.NOTEBOOK_NAME = notebook_name
        self.NOTEBOOK_FILE = f"{self.NOTEBOOK_NAME}.ipynb"
        self.WORKDIR = Path(f"./kaggle_notebook_{self.NOTEBOOK_NAME}")
        self.METADATA_PATH = self.WORKDIR / "kernel-metadata.json"
        self.WORKDIR.mkdir(exist_ok=True)

    """
    
    Create or update a notebook with the provided content and test it.
    
    """
    def create_update_test_notebook(self, content, isCreate):
        if isCreate:
            # create the notebook
            is_create_successful, tb_str = self.create_notebook(content)
            if isinstance(is_create_successful, Exception):
                raise Exception(str(tb_str))
        else:
            # back up the current notebook before appending new data
            self.backup_notebook()

            # append to the notebook
            is_update_complete, tb_str = self.append_cells_to_notebook(content)
            if isinstance(is_update_complete, Exception):
                raise Exception(str(tb_str))

        # test the notebook
        ex, tb_str = self.test_notebook()
        if isinstance(ex, Exception):
            raise Exception(str(tb_str))


    """
    
    Create a new notebook with the provided content
    
    """
    def create_notebook(self, notebook_content):
        print("Creating notebook...")

        notebook = self.read_notebook(self.NOTEBOOK_NAME)
        if notebook is None:
            print("No existing notebook found. A new one will be created.")

            # Create new notebook from full JSON content
            notebook = notebook_content
            is_write_complete = self.write_to_notebook(notebook, self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb")
            if is_write_complete is Exception:
                tb_str = traceback.format_exc()
                print("Failed to write to notebook.")
                return is_write_complete, tb_str
            
            print(f"Created new notebook {self.NOTEBOOK_NAME}")
            return True, None
        else:
            print("Notebook already exists. Use append function to add cells.")
            raise Exception("Notebook already exists. Use append function to add cells.")
    

    """
    
    Append new cells to the existing notebook

    """
    def append_cells_to_notebook(self, cell_contents):
        print("Appending to notebook...")

        notebook = self.read_notebook(self.NOTEBOOK_NAME)

        # Append the cells to the notebook
        for cell_content in cell_contents:
            notebook["cells"].append(cell_content)
            is_write_complete = self.write_to_notebook(notebook, self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb")
            if is_write_complete is Exception:
                tb_str = traceback.format_exc()
                print("Failed to write to notebook.")
                return is_write_complete, tb_str
        
        print(f"Appended cells to notebook {self.NOTEBOOK_NAME}")

        return True, None


    """
    
    Test the notebook execution using papermill

    """
    def test_notebook(self):
        print("Testing notebook execution...")
        
        #notebook_path, _ = self.backup_notebook()
        notebook_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb"
        notebook_output_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}-output.ipynb"

        try:
            print("Executing notebook with papermill...")

            pm.execute_notebook(notebook_path, notebook_output_path)

            print(f"Executed notebook and saved output to {notebook_output_path}")
        except Exception as e:
            print("An error occurred while executing the notebook:", e)
            tb_str = traceback.format_exc()
            self.revert_notebook()
            return e, tb_str

        return None, None


    """
    
    Push the notebook to Kaggle and monitor its status
    
    """
    def push_to_kaggle(self):
        metadata_created = self.create_metadata()
        if not metadata_created:
            print("Failed to create metadata. Cannot push to Kaggle.")
            raise Exception("Failed to create metadata. Cannot push to Kaggle.")
        
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
            return e


    """

    Read an existing notebook file and load its content 
    
    """
    def read_notebook(self, notebook_name):
        notebook_path = self.WORKDIR / f"{notebook_name}.ipynb"
        notebook = None

        print("Reading notebook from:", notebook_path)

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
            
            return True
        except Exception as e:
            print("An error occurred while writing to the notebook:", e)
            return e

    """

    Create a backup of the current notebook state

    """
    def backup_notebook(self):
        notebook_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb"
        backup_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}-backup.ipynb"

        shutil.copy(notebook_path, backup_path)
        print(f"Backup created at {backup_path}")
    
        return notebook_path, backup_path
    

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
            return True
        except Exception as e:
            print("An error occurred while creating metadata:", e)
            return False
