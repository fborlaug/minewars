import * as path from 'node:path';
import * as cdk from 'aws-cdk-lib/core';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecs_patterns from 'aws-cdk-lib/aws-ecs-patterns';
import { Platform } from 'aws-cdk-lib/aws-ecr-assets';
import * as rds from 'aws-cdk-lib/aws-rds';
import { Construct } from 'constructs';

export class MinewarsStack extends cdk.Stack {
  /** RDS security group - Step 10c will add ingress from the backend service. */
  public readonly dbSecurityGroup: ec2.SecurityGroup;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // --- VPC (public + isolated subnets, no NAT Gateway) ---
    const vpc = new ec2.Vpc(this, 'Vpc', {
      maxAzs: 2,
      natGateways: 0,
      subnetConfiguration: [
        { name: 'public', subnetType: ec2.SubnetType.PUBLIC, cidrMask: 24 },
        { name: 'isolated', subnetType: ec2.SubnetType.PRIVATE_ISOLATED, cidrMask: 24 },
      ],
    });

    // --- RDS security group ---
    this.dbSecurityGroup = new ec2.SecurityGroup(this, 'DbSecurityGroup', {
      vpc,
      description: 'RDS PostgreSQL - ingress added by backend service',
      allowAllOutbound: false,
    });

    // --- RDS PostgreSQL 17 ---
    const db = new rds.DatabaseInstance(this, 'Database', {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_17,
      }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO),
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      securityGroups: [this.dbSecurityGroup],
      credentials: rds.Credentials.fromGeneratedSecret('minewars'),
      databaseName: 'minewars',
      multiAz: false,
      allocatedStorage: 20,
      maxAllocatedStorage: 20,
      deleteAutomatedBackups: true,
      deletionProtection: false,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    // --- ECS Fargate backend service behind ALB ---
    const backend = new ecs_patterns.ApplicationLoadBalancedFargateService(this, 'Backend', {
      vpc,
      taskSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      assignPublicIp: true,
      cpu: 256,
      memoryLimitMiB: 512,
      desiredCount: 1,
      runtimePlatform: {
        cpuArchitecture: ecs.CpuArchitecture.ARM64,
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
      },
      taskImageOptions: {
        image: ecs.ContainerImage.fromAsset(path.join(__dirname, '..', '..', 'backend'), {
          platform: Platform.LINUX_ARM64,
        }),
        containerPort: 8080,
        environment: {
          QUARKUS_DATASOURCE_JDBC_URL: `jdbc:postgresql://${db.dbInstanceEndpointAddress}:5432/minewars`,
          MP_JWT_VERIFY_ISSUER: 'minewars',
          MINEWARS_JWT_EXPIRY: 'PT24H',
        },
        secrets: {
          QUARKUS_DATASOURCE_USERNAME: ecs.Secret.fromSecretsManager(db.secret!, 'username'),
          QUARKUS_DATASOURCE_PASSWORD: ecs.Secret.fromSecretsManager(db.secret!, 'password'),
        },
      },
    });

    // ALB health check
    backend.targetGroup.configureHealthCheck({
      path: '/q/health',
      healthyHttpCodes: '200',
    });

    // Allow backend Fargate tasks -> RDS on port 5432
    this.dbSecurityGroup.addIngressRule(
      backend.service.connections.securityGroups[0],
      ec2.Port.tcp(5432),
      'Allow backend Fargate service',
    );

    // --- Outputs ---
    new cdk.CfnOutput(this, 'DbEndpoint', {
      value: db.dbInstanceEndpointAddress,
      description: 'RDS PostgreSQL endpoint',
    });

    new cdk.CfnOutput(this, 'DbSecretArn', {
      value: db.secret!.secretArn,
      description: 'Secrets Manager ARN for DB credentials',
    });

    new cdk.CfnOutput(this, 'BackendUrl', {
      value: `http://${backend.loadBalancer.loadBalancerDnsName}`,
      description: 'ALB URL for the backend API',
    });
  }
}

