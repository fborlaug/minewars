#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib/core';
import { MinewarsStack } from '../lib/minewars-stack';

const app = new cdk.App();
new MinewarsStack(app, 'MinewarsStack', {
  env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: process.env.CDK_DEFAULT_REGION },
});
