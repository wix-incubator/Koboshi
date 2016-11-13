[![Build Status](https://travis-ci.org/wix/Koboshi.svg?branch=master)](https://travis-ci.org/wix/Koboshi)
# Koboshi
A library we use extensively at Wix for resilient caching of any configuration/resources that a service may need to rely on, but are obtained from an
unreliable source (such as other internal services, databases, or any other remote source).

A Highly Available Single Element Cache - When your application opts for availability over having up-to-date configuration.

Our cannonical use-case is our site renderer which has a very strict SLA.    
That service needs [feature-flags](https://github.com/wix/petri) in order to operate but it can't fail or be delayed if they aren't there.    
It can however work with stale feature flags which is where Koboshi steps in.

Koboshi makes sure that:
- Your service can always start up no matter the status of your remote source
- Your service is periodically updated with data from the remote source
- No user request has to incur the cost of updating that single-element cache

**NOTE:** As this is a work-in-progress, and is being changed as we get feedback from users, some interfaces might change or be removed at later versions.

### Usage

```scala
import com.wix.hoopoe.koboshi.cache.defaults.ResilientCaches
...
case class YourData(data: Seq[String])
val resilientCaches = ResilientCaches.resilientCaches(yourCacheFolder)
val resilientCache = resilientCaches.aResilientInitializedCache[YourData](
     new RemoteDataSource[YourData] {
        override def fetch(): YourData = ??? //e.g. read from a remote service
      })
val yourData: YourData = resilientCache.read()
```
**NOTE:** 
By default Koboshi uses [Jackson]() for working with the persistent copy.
It is advised not to use collections (Map, List, etc') directly as a DTO but rather encapsulate those in your own class as shown in the example above (YourData). This will allow better support for backward/forward compatability which otherwise will be compromised, leaving the local cached copy unparsable upon a change in the DTO.
Also, make sure the DTO is serializable. Koboshi supports Scala's case classes out of the box, but if you're using something else (Java for example) make sure to include an empty constructor and getters or other Jackson constructs (annotate with a custom @JSonCreator or @JsonDeserialize).

See all features in the [acceptance test](koboshi-bootstrap/src/it/java/com/wix/hoopoe/koboshi/ResilientCachesIT.scala).

### Maven

```xml
<dependency>
    <groupId>com.wix.hoopoe.koboshi</groupId>
    <artifactId>koboshi-bootstrap_2.11</artifactId>
    <version>0.0.4</version>
</dependency>
```

### SBT

```scala
libraryDependencies += "com.wix.hoopoe.koboshi" % "koboshi-bootstrap_2.11" % "0.0.4"
```

### Scala binary compatibility
This library is currently only published for Scala 2.11.x:

## License

Copyright (c) 2015 Wix.com Ltd. All Rights Reserved. Use of this source code is governed by a BSD-style license that can be found in the [LICENSE](LICENSE.md) file in the root of the source tree.

## Code of Conduct

Please note that this project is released with a Contributor [Code of Conduct](code_of_conduct.md). By participating in this project you agree to abide by its terms.

