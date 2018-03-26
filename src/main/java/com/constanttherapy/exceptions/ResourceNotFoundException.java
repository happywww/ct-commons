package com.constanttherapy.exceptions;

import java.io.FileNotFoundException;

/**
 * Created by Mahendra on 2017-10-03.
 */
public class ResourceNotFoundException extends FileNotFoundException
{
    private String resourceName;
    private String resourceFolderName = null;

    public ResourceNotFoundException(String resourceName)
    {
        assert (resourceName.length() > 0);
        this.resourceName = resourceName;
    }

    public ResourceNotFoundException(String resourceName, String resourceFolderName)
    {
        this.resourceName = resourceName;
        this.resourceFolderName = resourceFolderName;
    }

    @Override
    public String getMessage()
    {
        if (this.resourceFolderName != null)
            return String.format("Resource %s not found in folder %s", this.resourceName, this.resourceFolderName);
        else
            return String.format("Resource not found: %s", this.resourceName);
    }

}

