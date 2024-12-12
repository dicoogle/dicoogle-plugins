Dicoogle Plugins
===========================
Dicoogle is a modular platform that allows the integration of several plugins to extend its functionality. 
This repository contains the source code for the plugins that are distributed with the Dicoogle Platform.

Available Plugins
-----------------

### [Lucene Index/Query Plugin](plugins/lucene)
  
Plugin Based on lucene to support index and query of DICOM Meta-data.

### [File Storage Plugin](plugins/filestorage)

Plugin used in the storage of DICOM Files in the local file system. This plugin is necessary in order to use Dicoogle as a DICOM Storage Provider.

Our file storage plugin maps the DICOM hierarchical organization (Patient->Study->Series->Image) into a directory tree in the file system. Every object in the Dicoogle Platform may be traced back to its storage location by an URI, similar to `file:///tmp/file`. In order to support multiple providers, every Storage plugin must define a unique scheme, i.e protocol.

#### Settings Parameters

  * root-dir: is the root directory where DICOM Files will be stored (e.g. "/opt/dicoogle/dataset")
  * scheme: Specifies the scheme/protocol of the file plugin. This value is arbitrary, however it must be unique, as such avoid using well known protocol names such as http or file.
  * use-relative-path: Enabled by default, treats the store's root directory as the root of all stored item identifiers. For instance, if the root directory is in "/opt/dataset" and a file is stored in "/opt/dataset/CT/0001.dcm", then the file's URI will be "file:/CT/0001.dcm".


Configuring Plugins
-------------------

Plugins configurations are accessible via /DicoogleDir/Plugins/settings/PluginName.xml. Where PluginName stands for the name of the plugin.
Upon initialization, if no configurations file is supplied the Dicoogle Platform creates one with the default values.
