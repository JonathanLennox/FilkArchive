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
tier.  The ways I have found to do this are with the system
console (very tedious), with the aws cli, or with Cyberduck.

For the aws cli, the command looks like:

```
aws --endpoint-url https://s3.us-south.cloud-object-storage.appdomain.cloud/ --profile filkarchive s3api restore-object --bucket filkarchive --restore-request 'Days=7' --key [filename] 
```

From Cyberduck, you have to configure some [Cyberduck hidden configuration options](https://trac.cyberduck.io/wiki/help/en/howto/preferences#Hiddenconfigurationoptions):

```
s3.glacier.restore.expiration.days=7
s3.glacier.restore.tier=Bulk
```

(You should be able to the expiration days to any reasonable value that works for you up to 30; I haven't tested whether any other restore tier works.)

However you request it, the restore request takes up to 12 hours to complete the restoration from deep storage.
