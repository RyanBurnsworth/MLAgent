import json
from pathlib import Path
import subprocess

WORKDIR = Path(f"./datasets")

class DatasetService:


    def __init__(self):
        self.workdir = WORKDIR
        self.workdir.mkdir(exist_ok=True)


    """
    
    Download the hottest dataset using the search term
    
    """
    def download_dataset(self, search_term):
        cmd = [
            "kaggle", "datasets", "list",
            "--search", f"{search_term}",
            "--sort-by", "hottest"
        ]

        try:
            result = subprocess.run(cmd, capture_output=True, text=True, check=True)
            output = result.stdout

            if (output.lower == "no datasets found"):
                print("No datasets found.")
                return ""

            lines = output.strip().split("\n")

            data_lines = lines[2:]

            first_line = data_lines[0]

            dataset_name = first_line.split()[0]

            print("Dataset name:", dataset_name)

            download_path = Path(self.workdir) / dataset_name
            Path(download_path).mkdir(parents=True, exist_ok=True)

            subprocess.run(
                ["kaggle", "datasets", "download", dataset_name, "-p", download_path, "--unzip"],
                check=True
            )

            print(f"Dataset downloaded to {download_path}")

            datasets_paths = self.list_datasets(Path(download_path))
            if datasets_paths is None:
                print("No datasets found after download.")
                return None

            # get the dataset names without the .csv extension
            datasets = [p.stem for p in datasets_paths]

            print(f"Datasets downloaded: {datasets} ")

            dataset_details = self.get_dataset_details(dataset_name, datasets)
            if dataset_details is None:
                print("Failed to get dataset details.")
                return None
            return dataset_details
        except subprocess.CalledProcessError as e:
            print("An error occurred while downloading the dataset:", e)
            return None


    """
    
    Get detailed information about the dataset including title, description, datasets, headers, and top 25 rows
    
    """
    def get_dataset_details(self, dataset_name, datasets):
        try:
            # extract the dataset details
            data = self.get_dataset_manifest(dataset_name)
            if data is None:
                print("No manifest data found.")
                return None

            # build the full paths to each dataset file
            dataset_paths = []
            for dataset in datasets:
                dataset_path = self.workdir / dataset_name / f"{dataset}.csv"
                dataset_path = f".\\{dataset_path}"
                if dataset_path:
                    dataset_paths.append(dataset_path)

            record = {
                "dataset_name": dataset_name,
                "title": data.get("title", ""),
                "subtitle": data.get("subtitle", ""),
                "description": data.get("description", ""),
                "datasets": dataset_paths
            }

            print(f"Dataset details: {record}")

            return record
        except Exception as e:
            print("An error occurred while getting dataset details:", e)
            return None


    """
    
    Get the dataset manifest as a dictionary
    
    """
    def get_dataset_manifest(self, dataset_name):
        try:
            # downloads the datasets metadata
            subprocess.run(
                ["kaggle", "datasets", "metadata", dataset_name],
                check=True
            )

            with open("./dataset-metadata.json", "r", encoding="utf-8") as f:
                raw = f.read().strip()

            # Decode twice (since the file has JSON stored as a string)
            data = json.loads(raw)
            if isinstance(data, str):
                data = json.loads(data)
                return data
            
            return None
        except Exception as e:
            print("An error occurred while getting dataset manifest:", e)
            return None

    """
    
    Get the list of datasets in the directory
    
    """
    def list_datasets(self, dataset_path):
        try:
            csv_files = list(dataset_path.glob("*.csv"))
            if not csv_files:
                raise FileNotFoundError(f"No CSV files found in {dataset_path}")

            return csv_files
        except Exception as e:
            print("An error occurred while listing datasets:", e)
            return None
