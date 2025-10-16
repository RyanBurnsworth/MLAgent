import shutil
import time
import json
import subprocess
import nbformat
import papermill as pm
from pathlib import Path
import ast

class NotebookService:

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
    def create_or_append_notebook(self, notebook_data):
        notebook_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb"
        backup_path = self.WORKDIR / f"{self.NOTEBOOK_NAME}-backup.ipynb"
        
        notebook = self.read_notebook(self.NOTEBOOK_NAME)
        if notebook is None:
            print("No existing notebook found. A new one will be created.")

            # Create new notebook from full JSON content
            notebook = notebook_data
            is_write_complete = self.write_to_notebook(notebook, self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb")
            if is_write_complete is Exception:
                print("Failed to write to notebook.")
                return is_write_complete
            
            print(f"Created new notebook {self.NOTEBOOK_NAME}")
        else:
            notebook = notebook

            # Backup before modifying
            shutil.copy(notebook_path, backup_path)
            print(f"Backup created at {backup_path}")

            # Append the notebook data
            notebook["cells"].append(notebook_data)
            is_write_complete = self.write_to_notebook(notebook, self.WORKDIR / f"{self.NOTEBOOK_NAME}.ipynb")
            if is_write_complete is Exception:
                print("Failed to write to notebook.")
                return is_write_complete
            
            print(f"Appended cell to notebook {self.NOTEBOOK_NAME}")
            
        return True

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

        last_output = self.get_last_notebook_output(notebook_output_path)
        if last_output == "":
            raise Exception("Executed notebook has no output.")

        return last_output


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
            nb = nbformat.read(notebook_output_path, as_version=4)
        except Exception as e:
            print("An error occurred while reading the executed notebook:", e)
            return None

        last_output = None
        for cell in reversed(nb.cells):
            if cell.cell_type == 'code' and cell.outputs:
                last_output = cell.outputs[-1]
                break

        if last_output is not None:
            if last_output.output_type == 'stream':
                print(last_output.text)
                return last_output.text
            elif last_output.output_type in ['display_data', 'execute_result']:
                text = last_output.data.get('text/plain')
                if text:
                    try:
                        # Convert string representation of dict to actual dict
                        result = ast.literal_eval(text)
                        return result
                    except Exception as e:
                        print("Failed to parse notebook output:", e)
                        return text
        else:
            print("No output found in the notebook.")
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
