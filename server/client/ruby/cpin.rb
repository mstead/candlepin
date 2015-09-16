#! /usr/bin/env ruby

require 'active_support/all'
require 'awesome_print'
require 'highline/import'
require 'json'
require 'openssl'
require 'uri'
require 'yaml'
require 'hammer_cli'

require_relative './candlepin.rb'

VERSION = "1.0.0"

module HammerCLICandlepin
  module Util
    def warn(msg)
      say(HighLine.color("WARNING: #{msg}", :yellow))
    end

    def json_out(content)
      if @json_print_proc.nil?
        say(content.to_s)
      else
        @json_print_proc.call(content)
      end
    end
  end

  class X509Normalizer < HammerCLI::Options::Normalizers::File
    def format(cert)
      unless File.file?(cert)
        raise ArgumentError.new("The file '#{cert}' does not exist.")
      end
      begin
        return OpenSSL::X509::Certificate.new(File.read(cert))
      rescue => e
        raise ArgumentError.new("Could not read '#{cert}': #{e.message}")
      end
    end
  end

  class URINormalizer < HammerCLI::Options::Normalizers::AbstractNormalizer
    def format(uri)
      begin
        parsed_uri = URI.parse(uri)
      rescue URI::InvalidURIError => e
        raise ArgumentError.new(e.message)
      end
      # Don't allow FTP or Gopher or whatever else
      unless parsed_uri.kind_of?(URI::HTTP)
        raise ArgumentError.new("#{uri} is not an HTTP(S) URL")
      end
      # Just return the string because the uri is stored as a string
      # in the Settings object and we don't want to get things confused.
      return uri
    end
  end

  class CandlepinCommand < HammerCLI::AbstractCommand
    option(["-v", "--verbose"], :flag, _("be verbose"))
    option(["-d", "--debug"], :flag, _("show debugging output"))

    # The config file has already been read in by now, but we need to show it
    # in the help
    option(["--config"], "CFG_FILE", _("path to custom config file"),
      :default => DEFAULT_CONFIG_FILE,
    ) do |path|
      File.expand_path(path)
    end

    option(["-u", "--username"], "USERNAME", _("username to access the remote system"),
      :default => 'admin')

    option(["-p", "--password"], "PASSWORD", _("password to access the remote system"),
      :default => 'admin')

    option(["-s", "--server"], "SERVER", _("remote system address"),
      :default => 'https://localhost',
      :format => HammerCLICandlepin::URINormalizer.new)

    option(["-c", "--cacert"], "CERT", _("X509 CA certificate to use"),
      :format => HammerCLICandlepin::X509Normalizer.new)

    option(["--version"], :flag, _("show version")) do
      say("cpin #{VERSION}")
      exit HammerCLI::EX_OK
    end

    option(["--[no-]pretty-print"], :flag, _("pretty print JSON (or not)"), :default => true) do
        @json_print_proc = Proc.new do |content|
          # Negative indentation left-justifies the output
          formatted = content.awesome_inspect(:indent => -2, :index => false)
          say(formatted)
        end
    end

    option(["-i", "--insecure"], :flag, _("do not perform SSL/TLS verifications"), :default => false)

    def execute
      require 'pry'; binding.pry
      if !option_insecure? && option_server.end_with?("localhost") && option_cacert.nil?
        warn("You are connecting to localhost and didn't give a CA cert.  Assuming you want --insecure.")
        option_insecure = true  # NO_QA
        option_cacert = "blorp"
      end
    end

    def option_insecure=(val)
      puts "Assigning"
      @insecure = val
    end
  end

  class StatusCommand < CandlepinCommand
    include HammerCLICandlepin::Util

    def initialize(*args)
      super
      @global_options = {}
    end

    def get_client(global_options)
      server = global_options[:server]
      cacert = global_options[:cacert]
      args = {
        :host => server.host,
        :port => server.port,
        :context => server.path,
        :insecure => @insecure,
        :ca_path => cacert
      }

      client = NoAuthClient.new(args)
      client.debug if debug?
      client
    end

    def execute
      super
      puts "Hello world"
      puts "cacert is #{option_cacert}"
      puts "insecure is #{option_insecure?}"
      exit HammerCLI::EX_OK
      # json_out(@client.get_status.content)
    end
  end

  HammerCLICandlepin::CandlepinCommand.subcommand('status', 'Print server status', HammerCLICandlepin::StatusCommand)
end

#     def read_rc_file(cmd)
#       if cmd.data.key?(:rc_file)
#         rc_file = cmd.data[:rc_file]
#         error_if_missing = true
#       else
#         rc_file = File.join(ENV['HOME'], '.cpinrc')
#         error_if_missing = false
#       end

#       if File.exist?(rc_file)
#         @rc_settings = YAML.load(File.open(rc_file, 'r'))
#         @rc_settings.deep_symbolize_keys!
#       elsif error_if_missing
#         error("Cannot find #{rc_file}!")
#         raise CmdParse::InvalidArgumentError.new
#       end
#     end
#   end
# end



    # @@rc_whitelist = [:debug, :verbose]

    # DEFAULT_RC = File.join(ENV['HOME'], '.cpinrc')
      # def read_rc_file(rc_file)
      #   if File.exist?(rc_file)
      #     rc_settings = YAML.load(File.open(rc_file, 'r'))
      #     rc_settings.deep_symbolize_keys!
      #   else
      #     say(rc_file)
      #     say_warning("Cannot find #{rc_file}!") unless rc_file == DEFAULT_RC
      #     rc_settings = {}
      #   end

      #   bad_settings = []
      #   rc_settings.each do |k, v|
      #     if @@rc_whitelist.include?(k)
      #       send(:"#{k}=", v)
      #     else
      #       bad_settings << k
      #     end
      #   end
      #   unless bad_settings.empty?
      #     say_error("The cpinrc file does not support these settings: #{bad_settings}")
      #   end
      # end
  # end
# end
