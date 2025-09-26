import time
import json
import subprocess
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
        self.NOTEBOOK_FILE = f"{notebook_name}.ipynb"
        self.WORKDIR = Path(f"./kaggle_notebook_{notebook_name}")
        self.METADATA_PATH = Path(self.WORKDIR + "/kernel-metadata.json")
        self.WORKDIR.mkdir(exist_ok=True)

        self.create_metadata()

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
        
        with open(self.METADATA_PATH, "w") as f:
            json.dump(kernel_metadata, f, indent=2)
        print(f"Created metadata at {self.METADATA_PATH}")

    """

    Create a Jupyter notebook file with the given content

    """
    def create_notebook(self, notebook_content):
        notebook_path = self.WORKDIR / self.NOTEBOOK_FILE

        with open(notebook_path, "w") as f:
            json.dump(notebook_content, f, indent=2)
        
        print(f"Created notebook at {notebook_path}")

    """
    
    Push the notebook to Kaggle and monitor its status
    
    """
    def push_to_kaggle(self):
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
            return ""
