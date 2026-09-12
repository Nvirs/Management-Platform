variable "name" {
  description = "Base name for tagging VPC resources"
  type        = string
}

variable "cluster_name" {
  description = "EKS cluster name that will live in this VPC (used for the kubernetes.io/cluster subnet tag)"
  type        = string
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "azs" {
  description = "Availability zones to spread subnets across (EKS requires at least 2)"
  type        = list(string)
}

variable "public_subnet_cidrs" {
  type = list(string)
}

variable "private_subnet_cidrs" {
  type = list(string)
}

variable "single_nat_gateway" {
  description = "Use one shared NAT Gateway instead of one per AZ"
  type        = bool
  default     = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
