.. _community_geopkg:

GeoPackage Extension
====================
This plugin brings in the ability to write GeoPackage files in GeoServer using WPS. Reading GeoPackage files is part of the core functionality of GeoServer, and does not require this extension.

For WMS and WFS GeoPackage output, see the :ref:`GeoPKG Output<geopkgoutput>` extension.


`GeoPackage <http://www.opengeospatial.org/projects/groups/geopackageswg/>`_ is an SQLite based standard format that is able to hold multiple vector and raster data layers in a single file.

The GeoServer GeoPackage extension also allows to create a completely custom made GeoPackage with multiple layers, using the GeoPackage process.

.. toctree::
   :maxdepth: 2

   installing
   output
