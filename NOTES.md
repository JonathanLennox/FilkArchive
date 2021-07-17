# Notes on tricks I've used

## Aborting multipart uploads

To generate the commands to abort multipart uploads to IBM cloud storage:
```
ibmcloud cos multipart-uploads --output json --bucket filkarchive | jq -r '.Uploads[] | select(."Initiated" <= "2021-07-15") | "ibmcloud cos abort-multipart-upload --bucket=filkarchive --key=\"\(.Key)\" --upload-id=\"\(.UploadId)\""'
```

You'll need the [ibmcloud cli](https://www.ibm.com/cloud/cli) installed.

## Setting up aws cli to access ibmcloud

Set up an aws profile using `aws configure --profile filkarchive`

The Access Key ID and the Secret Access Keys come from `cos_hmac_keys` in the IBMCloud Console Service credentials

Then, run the aws cli with
```
aws --endpoint-url https://s3.us-south.cloud-object-storage.appdomain.cloud/ --profile filkarchive [command]
```

## Restoring files from the archive tier

To do anything with files in the cloud storage (download them, move
them, rename them, etc.) they need to be restored from the archive
tier.  The only ways I have found to do this are with the system
console (very tedious), or with the aws cli.

For the aws cli, the command looks like:

```
aws --endpoint-url https://s3.us-south.cloud-object-storage.appdomain.cloud/ --profile filkarchive s3api restore-object --bucket filkarchive --restore-request 'Days=7' --key [filename] 
```

The restore request takes up to 12 hours to complete the restoration from deep storage.