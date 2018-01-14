package com.github.sioncheng.push.conf

case class HBaseStorageConfig(quorum: String, port: Int)
case class ElasticSearchServerConfig(host: String, port:Int, clusterName: String)
