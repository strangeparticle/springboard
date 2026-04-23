package com.strangeparticle.springboard.app.domain

fun SpringboardSource.S3Source.toHttpsUrl(): String =
    "https://$bucket.s3.amazonaws.com/$key"
