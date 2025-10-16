package com.ryanburnsworth.mlagent.mlagent.util;

import org.springframework.ai.chat.prompt.PromptTemplate;

public class Prompts {
    public static final PromptTemplate DATA_LOADING_PROMPT = new PromptTemplate("""
                    Create a Jupyter notebook cell that loads and inspects a dataset. Your task:
            
                    You are provided with the following dataset metadata:
            
                    Title: {title}
                    Subtitle: {subtitle}
                    Description: {description}
                    Dataset files: {datasets}
            
                    1. Load the training and testing datasets from the Dataset files in the dataset metadata. 
                        If only one dataset, use only training. Use the exact filenames given in Dataset files.
                    2. Display the first few rows of the training set using head()
                    3. Show the training dataset info using info()
                    4. Provide statistical summary on the training set using describe()
                    5. Print the training dataset shape and column names
                    6. Identify the target variable (look for common names like 'target', 'label', 'class', 'y', or the last column)
                    7. Include Markdown cells:
                    8. At the top: introduce the dataset and the problem to solve.
                    9. Detect if a GPU is available (using PyTorch or TensorFlow) and configure the environment to use the GPU. 
                        If no GPU is available, default to CPU. Include the detected device in the dataset summary.
            
                    Requirements:
                    - Import all necessary libraries (pandas, numpy, etc.)
                    - Handle common file formats (CSV, JSON, Excel)
                    - Include error handling for file loading
                    - Print key information clearly:
                      - Dataset shape
                      - Column names and data types
                      - Missing value counts per column
                      - Suspected target variable name
                      - Sample of the data
                      - Output must be in a valid ipynb format.
                      - Do not add additional text or wrap the code in ```markdown or json ```
                    Store the loaded training dataset in a variable called 'df' for the next step.
                    Store the loaded validation dataset, if any, in a variable called 'test_df' for the next step.
                    GPU must be used automatically if available for any future ML tasks.
            
                    dataset_summary = \\{
                        "shape": dataset shape,
                        "columns": number of columns,
                        "missing_values": are there missing values,
                        "sample_data": dataset head,
                        "statistical_summary": dataset description,
                        "info": dataset info
                    \\}
                    ONLY RETURN THIS JSON OBJECT as the final output. Do not return empty notebook cells, extra code, or unrelated comments.
            """);

    public static final PromptTemplate ERROR_HANDLING_PROMPT = new PromptTemplate("""
        There was an error in the code you provided. Fix the errors provided and output a valid output.
    
        You will be provided with:
    
        userPrompt: {userPrompt} - the user's prompt
        aiResponse: {aiResponse} - your code response to the user
        errorMessage: {errorMessage} - the generic error message
        errorDetails: {errorDetails} - the stack trace
    
        Requirements:
            - Rewrite the fixed code in its entirety. Not just the fixes.
            - Output must be in a valid ipynb code format. Either an entire ipynb notebook or cells to be appended within an existing notebook depending on the aiResponse given above.
            - Do not add additional text or comments. Return only the fixed code
    """);
}
