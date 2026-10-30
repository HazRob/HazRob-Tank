# GHS01–GHS09 offline Android vector pictograms — source and license

This directory includes `ghs_01.xml` through `ghs_09.xml`, converted by HazRob into
Android VectorDrawable resources from SVG pictograms in:

- Upstream source: https://github.com/karlnorling/ghs-hazard-pictograms
- Asset paths: `packages/@ghs-hazard-pictograms/assets/assets/`
- Original metadata: nine GHS pictograms adapted from UNECE/Wikimedia Commons (public-domain symbols).
- Specification: https://unece.org/transport/dangerous-goods/ghs-pictograms
- OSHA display definitions: https://www.osha.gov/hazcom/pictograms

The GHS symbol content is officially standardized. MIT license notice for the
upstream asset-collection distribution (verbatim below) is preserved in the source
repository and should be retained in derivative distributions.

MIT License

Copyright (c) 2024 Karl Norling

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

Product semantics:
- GHS pictograms reflect **chemical hazard classifications** only where
  classification and an original pictogram code have scientific provenance.
- GHS09 environmental icon is part of GHS but not mandatory under US OSHA HCS.
- Radiation and simple oxygen displacement are not among the nine GHS icons:
  these require distinct signage and dedicated sensing.
- A pictogram beside an unverified candidate MUST be marked conditional and
  MUST NOT be interpreted as an actual detection or certified ppm alarm.
