from pathlib import Path
import subprocess

# Run the command
cmd = [
    "kaggle", "datasets", "list",
    "--search", "ufo-sightings",
    "--sort-by", "hottest"
]

result = subprocess.run(cmd, capture_output=True, text=True, check=True)
output = result.stdout

if (output.lower == "no datasets found"):
    print("No datasets found.")
    exit(1)

# Split into lines
lines = output.strip().split("\n")

# Remove header lines (first 2 lines)
data_lines = lines[2:]

# Take first dataset line
first_line = data_lines[0]

# The first column is the dataset ref
first_ref = first_line.split()[0]

print("First dataset ref:", first_ref)

# Step 2: Download
download_path = "./"
Path(download_path).mkdir(parents=True, exist_ok=True)
subprocess.run(
    ["kaggle", "datasets", "download", first_ref, "-p", download_path, "--unzip"],
    check=True
)

print(f"Dataset downloaded to {download_path}")
